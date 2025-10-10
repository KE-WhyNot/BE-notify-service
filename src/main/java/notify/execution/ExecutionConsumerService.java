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

    /** Debezium topic: finance.finance.execution -> 개인 알림 생성 */
    @KafkaListener(topics = "finance.finance.execution", containerFactory = "kafkaListenerContainerFactory")
    public void onExecution(String value, Acknowledgment ack) {
        try {
            JsonNode root = om.readTree(value);
            String op = root.path("payload").path("op").asText("");
            if (!op.equals("c") && !op.equals("u")) { ack.acknowledge(); return; }

            JsonNode a = root.path("payload").path("after");
            if (a.isMissingNode() || a.isNull()) { ack.acknowledge(); return; }

            Long userId = asLongOrNull(a, "userId");
            if (userId == null || userId <= 0) { ack.acknowledge(); return; }

            long executionId  = a.path("executionId").asLong();
            String stockId    = asTextOrNull(a, "stockId");
            int isBuy         = a.path("isBuy").asInt(0); // 1=매수, 0=매도
            long qty          = a.path("quantity").asLong(0);
            String priceStr   = asTextOrNull(a, "price");
            String totalStr   = asTextOrNull(a, "totalPrice");
            String tradeAtStr = asTextOrNull(a, "tradeAt"); // 'YYYY-MM-DD HH:mm:ss'

            // Title/Message (English)
            String side   = isBuy == 1 ? "BUY" : "SELL";
            String title  = (isBuy == 1 ? "Buy Filled: " : "Sell Filled: ") + safe(stockId, "N/A") + " x" + qty;
            // "YYYY-MM-DD HH:mm  AAPL  BUY  3 shares @ 149.50"
            String message = String.format("%s  %s  %s  %d shares @ %s",
                    tradeAtStr != null ? left(tradeAtStr, 16) : "",
                    safe(stockId, "N/A"),
                    side,
                    qty,
                    safe(priceStr, "0"));

            int n = jdbc.update("""
                INSERT IGNORE INTO notify.notification_event
                  (userId, type, title, message, data, dedupKey, createdAt)
                VALUES
                  (?, 'EXECUTION', ?, ?,
                   JSON_OBJECT(
                     'executionId', ?, 'stockId', ?, 'isBuy', ?,
                     'qty', ?, 'price', ?, 'totalPrice', ?, 'tradeAt', ?
                   ),
                   CONCAT('exec:', ?, ':', ?),
                   NOW())
            """,
            userId, title, message,
            executionId, stockId, isBuy,
            qty, priceStr, totalStr, tradeAtStr,
            executionId, userId);

            log.info("EXECUTION notify inserted userId={} executionId={} rows={}", userId, executionId, n);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Execution consume failed: {}", e.toString(), e);
            // ack 생략 → 재처리
        }
    }


    // Helper methods
    private Long asLongOrNull(JsonNode node, String field) {
        JsonNode fieldNode = node.path(field);
        return fieldNode.isNull() || fieldNode.isMissingNode() ? null : fieldNode.asLong();
    }

    private String asTextOrNull(JsonNode node, String field) {
        JsonNode fieldNode = node.path(field);
        return fieldNode.isNull() || fieldNode.isMissingNode() ? null : fieldNode.asText();
    }


    private String safe(String str, String defaultValue) {
        return str != null && !str.trim().isEmpty() ? str : defaultValue;
    }

    private String left(String str, int len) {
        if (str == null || str.length() <= len) return str;
        return str.substring(0, len);
    }
}
