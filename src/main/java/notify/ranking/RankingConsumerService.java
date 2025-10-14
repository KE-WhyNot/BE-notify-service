package notify.ranking;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingConsumerService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Kafka topic: notify.ranking.top10 */
    @KafkaListener(topics = "notify.ranking.top10", containerFactory = "kafkaListenerContainerFactory")
    public void consume(String message, Acknowledgment ack) {
        try {
            // 메시지는 [{"userId":1,"rank":3,"profitRate":21.4}, ...] 형태
            List<RankItem> top10 = objectMapper.readValue(message, new TypeReference<>() {});
            log.info("📩 Received Top10 ranking: {}", top10);

            for (RankItem item : top10) {
                String userId = item.getUserId();
                int rank = item.getRank();
                double profitRate = item.getProfitRate();

                String title = String.format("🎉 실시간 수익률 랭킹 %d위 진입!", rank);
                String msg = String.format("현재 수익률: %.2f%%", profitRate);
                String data = String.format("{\"userId\":\"%s\",\"rank\":%d,\"profitRate\":%.2f}",
                        userId, rank, profitRate);
                String dedupKey = String.format("rank:%s:%d", userId, rank);

                // SQL INSERT
                int n = jdbc.update("""
                    INSERT IGNORE INTO notify.notification_event
                      (userId, type, title, message, data, dedupKey, createdAt)
                    VALUES
                      (?, 'RANKING', ?, ?, ?, ?, NOW())
                """,
                userId, title, msg, data, dedupKey);

                if (n > 0)
                    log.info("✅ RANKING notify inserted userId={} rank={} rows={}", userId, rank, n);
                else
                    log.info("⚠️ RANKING notify skipped (duplicate) userId={} rank={}", userId, rank);
            }

            ack.acknowledge(); // Kafka offset 커밋
        } catch (Exception e) {
            log.error("❌ Failed to process ranking message: {}", message, e);
            // ack 생략 → 재처리
        }
    }
}
