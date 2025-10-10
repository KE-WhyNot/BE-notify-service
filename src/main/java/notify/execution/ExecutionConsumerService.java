package notify.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notify.domain.notify.domain.entity.Notification;
import notify.domain.notify.domain.entity.NotificationType;
import notify.domain.notify.domain.repository.NotificationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionConsumerService {
    private final JdbcTemplate jdbc;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper om = new ObjectMapper();

    /** Debezium topic: finance.finance.execution -> trade_execution UPSERT + 개인 알림 생성 */
    @KafkaListener(topics = "finance.finance.execution", containerFactory = "kafkaListenerContainerFactory")
    public void onExecution(String value, Acknowledgment ack) {
        try {
            JsonNode root = om.readTree(value);
            String op = root.path("payload").path("op").asText("");
            if (!op.equals("c") && !op.equals("u")) { ack.acknowledge(); return; }

            JsonNode a = root.path("payload").path("after");
            if (a.isMissingNode() || a.isNull()) { ack.acknowledge(); return; }

            long executionId = a.path("executionId").asLong();
            long userId      = a.path("userId").asLong();
            String stockId   = a.path("stockId").asText();
            Long sectorId    = a.path("sectorId").isNull() ? null : a.path("sectorId").asLong();
            long tradeAtMs   = a.path("tradeAt").asLong(); // millis
            int isBuy        = a.path("isBuy").asInt();
            int quantity     = a.path("quantity").asInt();
            String price     = a.path("price").asText("0.0000");
            String totalPrice= a.path("totalPrice").isNull() ? null : a.path("totalPrice").asText();

            // 1) trade_execution UPSERT
            jdbc.update("""
                INSERT INTO notify.trade_execution
                  (executionId, userId, stockId, sectorId, tradeAt, isBuy, quantity, price, totalPrice, createdAt)
                VALUES (?, ?, ?, ?, FROM_UNIXTIME(?/1000), ?, ?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE
                  userId=VALUES(userId), stockId=VALUES(stockId), sectorId=VALUES(sectorId),
                  tradeAt=VALUES(tradeAt), isBuy=VALUES(isBuy), quantity=VALUES(quantity),
                  price=VALUES(price), totalPrice=VALUES(totalPrice)
            """, executionId, userId, stockId, sectorId, tradeAtMs, isBuy, quantity, price, totalPrice);

            // 2) 개인 알림 생성 (JPA 엔티티 사용)
            createNotification(userId, executionId, stockId, isBuy, quantity, price, totalPrice, tradeAtMs);

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Execution consume failed: {}", e.toString(), e);
        }
    }

    @Transactional
    public void createNotification(Long userId, Long executionId, String stockId, int isBuy, int quantity, String price, String totalPrice, long tradeAtMs) {
        try {
            // 중복 확인
            String dedupKey = "exec:" + executionId;
            boolean exists = notificationRepository.findByDedupKey(dedupKey).isPresent();
            if (exists) {
                log.debug("Notification already exists for executionId: {}", executionId);
                return;
            }

            // 시간 변환
            Instant tradeAt = Instant.ofEpochMilli(tradeAtMs);
            String tradeAtFormatted = tradeAt.atZone(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            // 제목과 메시지 생성
            String title = (isBuy == 1 ? "매수 체결" : "매도 체결") + ": " + stockId + " x" + quantity;
            String message = tradeAtFormatted + " " + stockId + " " + 
                    (isBuy == 1 ? "매수" : "매도") + " " + quantity + "주 @ " + price;

            // JSON 데이터 생성
            String data = String.format("""
                {
                    "executionId": %d,
                    "stockId": "%s",
                    "sectorId": 1,
                    "isBuy": %d,
                    "qty": %d,
                    "price": "%s",
                    "totalPrice": "%s",
                    "tradeAt": "%s"
                }
                """, executionId, stockId, isBuy, quantity, price, 
                totalPrice != null ? totalPrice : "0", 
                tradeAt.atZone(ZoneId.of("Asia/Seoul"))
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // 알림 엔티티 생성 및 저장
            Notification notification = Notification.builder()
                    .userId(userId)
                    .type(NotificationType.EXECUTION)
                    .title(title)
                    .message(message)
                    .data(data)
                    .dedupKey(dedupKey)
                    .build();

            notificationRepository.save(notification);
            log.info("Notification created for executionId: {}, userId: {}", executionId, userId);

        } catch (Exception e) {
            log.error("Failed to create notification for executionId: {}, error: {}", executionId, e.getMessage(), e);
        }
    }
}
