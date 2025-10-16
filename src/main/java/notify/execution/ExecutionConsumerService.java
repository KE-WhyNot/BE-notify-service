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

            String userId = asTextOrNull(a, "user_id");
            if (userId == null || userId.trim().isEmpty()) { ack.acknowledge(); return; }

            long executionId  = a.path("execution_id").asLong();
            String stockId    = asTextOrNull(a, "stock_id");
            String stockName  = asTextOrNull(a, "stock_name_snapshot"); // 종목명 추가
            int isBuy         = a.path("is_buy").asInt(0); // 1=매수, 0=매도
            long qty          = a.path("quantity").asLong(0);
            String priceStr   = asTextOrNull(a, "price");
            String totalStr   = asTextOrNull(a, "total_price");
            String tradeAtStr = asTextOrNull(a, "trade_at"); // 'YYYY-MM-DD HH:mm:ss'

            // Title/Message (Korean)
            String side   = isBuy == 1 ? "매수" : "매도";
            String displayName = safe(stockName, safe(stockId, "N/A")); // 종목명 우선, 없으면 종목코드
            String title  = (isBuy == 1 ? "매수 체결: " : "매도 체결: ") + displayName + " x" + qty;
            // "매수 체결: 삼성전자 x1"
            String message = String.format("%s %d주 %s원 %s 체결됐습니다!",
                    displayName,
                    qty,
                    safe(priceStr, "0"),
                    side);

            int n = jdbc.update("""
                INSERT IGNORE INTO notify.notification_event
                  (user_id, type, title, message, data, dedup_key, created_at)
                VALUES
                  (?, 'EXECUTION', ?, ?,
                   JSON_OBJECT(
                     'execution_id', ?, 'stock_id', ?, 'stock_name', ?, 'is_buy', ?,
                     'qty', ?, 'price', ?, 'total_price', ?, 'trade_at', ?
                   ),
                   CONCAT('exec:', ?, ':', ?),
                   NOW())
            """,
            userId, title, message,
            executionId, stockId, stockName, isBuy,
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
    private String asTextOrNull(JsonNode node, String field) {
        JsonNode fieldNode = node.path(field);
        return fieldNode.isNull() || fieldNode.isMissingNode() ? null : fieldNode.asText();
    }

    private String safe(String str, String defaultValue) {
        return str != null && !str.trim().isEmpty() ? str : defaultValue;
    }
}
