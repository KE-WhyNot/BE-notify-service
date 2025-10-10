package notify.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionConsumerService {
    private final JdbcTemplate jdbc;
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

            // 2) 개인 알림 INSERT (멱등: (userId, dedupKey) 유니크)
            String title = (isBuy==1? "매수 체결: " : "매도 체결: ") + stockId;
            String msg   = quantity + "주 @ " + price + (totalPrice!=null? " (총 " + totalPrice + ")" : "");
            String data  = """
                {"executionId":%d,"stockId":"%s","isBuy":%d,"quantity":%d,"price":"%s"}
            """.formatted(executionId, stockId, isBuy, quantity, price);
            jdbc.update("""
                INSERT IGNORE INTO notify.notification_event
                  (userId, type, title, message, data, dedupKey, createdAt)
                VALUES (?, 'EXECUTION', ?, ?, ?, CONCAT('exec:', ?), NOW())
            """, userId, title, msg, data, executionId);

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Execution consume failed: {}", e.toString(), e);
        }
    }
}
