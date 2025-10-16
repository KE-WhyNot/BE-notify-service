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

import java.util.*;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingConsumerService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper; // 주입받기 (SNAKE_CASE)

    /** Kafka topic: notify.ranking.top10 */
    @KafkaListener(topics = "notify.ranking.top10", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void consume(String message, Acknowledgment ack) {
        try {
            // 메시지는 [{"user_id":"1","rank_no":3,"profit_rate":21.4}, ...]
            List<RankItem> top10 = objectMapper.readValue(message, new TypeReference<>() {});
            log.info("📩 Received Top10 ranking (raw): {}", top10);

            // --- ① 페이로드 유효성 검사 ---
            if (top10 == null) top10 = List.of();

            // user_id가 비어있거나 null인 항목 제거
            List<RankItem> validTop10 = top10.stream()
                    .filter(it -> it.getUserId() != null && !it.getUserId().isBlank())
                    .collect(Collectors.toList());

            // 수치값 NaN/무한대 방지
            validTop10.forEach(it -> {
                double r = it.getProfitRate();
                if (Double.isNaN(r) || Double.isInfinite(r)) it.setProfitRate(0.0);
            });

            // rank_no 정합성 보장 (옵션): 들어온 순서/정렬 기준에 따라 1..N 재부여
            for (int i = 0; i < validTop10.size(); i++) {
                validTop10.get(i).setRankNo(i + 1);
            }

            // 비정상/빈 리스트면 DB에 손대지 않고 종료 (업스트림 장애 보호)
            if (validTop10.isEmpty()) {
                log.warn("⚠️ Skip apply: empty/invalid ranking payload -> no DB change");
                ack.acknowledge();
                return;
            }

            // --- ② 업서트 ---
            jdbc.batchUpdate("""
                    INSERT INTO notify.ranking_top10 (user_id, rank_no, profit_rate, updated_at)
                    VALUES (?, ?, ?, NOW())
                    ON DUPLICATE KEY UPDATE
                      rank_no = VALUES(rank_no),
                      profit_rate = VALUES(profit_rate),
                      updated_at = NOW()
                """,
                    validTop10,
                    validTop10.size(),
                    (ps, item) -> {
                        ps.setString(1, item.getUserId());
                        ps.setInt(2, item.getRankNo());
                        ps.setDouble(3, item.getProfitRate());
                    }
            );

            // --- ③ 이탈자 삭제 (안전하게) ---
            Set<String> incomingSet = validTop10.stream()
                    .map(RankItem::getUserId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            String placeholders = String.join(",", Collections.nCopies(incomingSet.size(), "?"));
            String sql = "DELETE FROM notify.ranking_top10 WHERE user_id NOT IN (" + placeholders + ")";

            jdbc.update(con -> {
                var ps = con.prepareStatement(sql);
                int idx = 1;
                for (String u : incomingSet) ps.setString(idx++, u);
                return ps;
            });

            // --- ④ 순위 알림 (중복 방지 dedup_key) ---
            for (RankItem item : validTop10) {
                String userId = item.getUserId();
                int rankNo = item.getRankNo();
                double profitRate = item.getProfitRate();

                String title = String.format("🎉 실시간 수익률 랭킹 %d위 진입!", rankNo);
                String msg = String.format("현재 수익률: %.2f%%", profitRate);
                String data = String.format("{\"user_id\":\"%s\",\"rank_no\":%d,\"profit_rate\":%.2f}",
                        userId, rankNo, profitRate);
                String dedupKey = String.format("rank:%s:%d", userId, rankNo);

                jdbc.update("""
                    INSERT IGNORE INTO notify.notification_event
                      (user_id, type, title, message, data, dedup_key, created_at)
                    VALUES
                      (?, 'RANKING', ?, ?, ?, ?, NOW())
                """, userId, title, msg, data, dedupKey);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("❌ Failed to process ranking message: {}", message, e);
            // 트랜잭션 롤백 후 재시도
            throw new RuntimeException(e);
        }
    }
}