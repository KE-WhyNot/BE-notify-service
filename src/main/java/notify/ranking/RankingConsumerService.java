package notify.ranking;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingConsumerService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "notify.ranking.top10", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void consume(String message, Acknowledgment ack) {
        try {
            // 0) 안전 파싱 + 필터
            List<RankItem> raw = objectMapper.readValue(message, new TypeReference<>() {});
            List<RankItem> top10 = raw.stream()
                    .filter(it -> it != null
                            && it.getUserId() != null && !it.getUserId().isBlank()
                            && it.getRankNo() > 0
                            && !Double.isNaN(it.getProfitRate()))
                    .toList();

            if (top10.isEmpty()) {
                log.warn("⚠️ Empty/invalid Top10 payload. Skip patch to protect snapshot.");
                ack.acknowledge();
                return;
            }

            // 1) 업서트 (MySQL 8.0.20+ 권장 문법: AS new 사용)
            jdbc.batchUpdate("""
                INSERT INTO notify.ranking_top10_v2
                  (user_id, rank_no, profit_rate, in_top10, last_seen_at, updated_at)
                VALUES (?, ?, ?, 1, NOW(), NOW())
                AS new
                ON DUPLICATE KEY UPDATE
                  rank_no      = new.rank_no,
                  profit_rate  = new.profit_rate,
                  in_top10     = 1,
                  last_seen_at = NOW(),
                  updated_at   = NOW()
            """,
                    top10, top10.size(),
                    (ps, item) -> {
                        ps.setString(1, item.getUserId());
                        ps.setInt(2, item.getRankNo());
                        ps.setDouble(3, item.getProfitRate());
                    });

            // 2) 이번 리스트에 없는 기존 활성 유저들만 비활성화 (삭제 금지)
            //    - 중복 제거 & 순서 유지: LinkedHashSet
            Set<String> incoming = new LinkedHashSet<>(top10.stream().map(RankItem::getUserId).toList());
            if (!incoming.isEmpty()) {
                String placeholders = String.join(",", incoming.stream().map(x -> "?").toList());
                String sql = "UPDATE notify.ranking_top10_v2 " +
                             "   SET in_top10=0, dropped_at=IFNULL(dropped_at, NOW()), updated_at=NOW() " +
                             " WHERE in_top10=1 AND user_id NOT IN (" + placeholders + ")";
                jdbc.update(con -> {
                    var ps = con.prepareStatement(sql);
                    int i = 1;
                    for (String u : incoming) ps.setString(i++, u);
                    return ps;
                });
            }

            // 3) 알림(중복 방지 dedup_key)
            for (RankItem item : top10) {
                String userId = item.getUserId();
                int rankNo = item.getRankNo();
                double profitRate = item.getProfitRate();

                String title = String.format("🎉 실시간 수익률 랭킹 %d위", rankNo);
                String msg = String.format("현재 수익률: %.2f%%", profitRate);
                String data = String.format("{\"user_id\":\"%s\",\"rank_no\":%d,\"profit_rate\":%.2f}",
                        userId, rankNo, profitRate);
                String dedupKey = String.format("rank:%s:%d", userId, rankNo);

                jdbc.update("""
                    INSERT IGNORE INTO notify.notification_event
                        (user_id, type, title, message, data, dedup_key, created_at)
                    VALUES (?, 'RANKING', ?, ?, ?, ?, NOW())
                """, userId, title, msg, data, dedupKey);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("❌ Failed to process ranking message: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}
