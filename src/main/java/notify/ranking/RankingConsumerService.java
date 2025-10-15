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

import java.util.HashSet;
import java.util.List;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingConsumerService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Kafka topic: notify.ranking.top10 */
    @KafkaListener(topics = "notify.ranking.top10", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void consume(String message, Acknowledgment ack) {
        try {
            // 메시지는 [{"userId":"1","rank":3,"profitRate":21.4}, ...]
            List<RankItem> top10 = objectMapper.readValue(message, new TypeReference<>() {});
            log.info("📩 Received Top10 ranking: {}", top10);

            // 현재 스냅샷 읽기 (이탈자 감지)
            List<String> currentUsers = jdbc.query(
                    "SELECT user_id FROM notify.ranking_top10",
                    (rs, i) -> rs.getString(1)
            );

            // 이번 수신 Top10의 userId 
            Set<String> incomingSet = new HashSet<>();
            for (RankItem item : top10) incomingSet.add(item.getUserId());

            // 업서트(존재하면 업데이트, 없으면 인서트)
            jdbc.batchUpdate("""
                    INSERT INTO notify.ranking_top10 (user_id, rank_no, profit_rate, updated_at)
                    VALUES (?, ?, ?, NOW())
                    ON DUPLICATE KEY UPDATE
                      rank_no = VALUES(rank_no),
                      profit_rate = VALUES(profit_rate),
                      updated_at = NOW()
                """,
                top10,
                top10.size(),
                (ps, item) -> {
                    ps.setString(1, item.getUserId());
                    ps.setInt(2, item.getRankNo());
                    ps.setDouble(3, item.getProfitRate());
                }
            );

            //스냅샷에서 제외된 사용자 삭제 (이번 리스트에 없는 사람)
        
            if (incomingSet.isEmpty()) {
                jdbc.update("DELETE FROM notify.ranking_top10");
            } else {
                String placeholders = String.join(",", incomingSet.stream().map(x -> "?").toList());
                String sql = "DELETE FROM notify.ranking_top10 WHERE user_id NOT IN (" + placeholders + ")";
                jdbc.update(con -> {
                    var ps = con.prepareStatement(sql);
                    int idx = 1;
                    for (String u : incomingSet) ps.setString(idx++, u);
                    return ps;
                });
            }

            //  순위 알림
            for (RankItem item : top10) {
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
            // ack 생략 → 재처리
            throw new RuntimeException(e); // 트랜잭션 롤백
        }
    }
}