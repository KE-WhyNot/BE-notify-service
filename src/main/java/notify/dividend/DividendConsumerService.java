package notify.dividend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class DividendConsumerService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper om = new ObjectMapper();
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @KafkaListener(topics = "finance.finance.dividendInfo", containerFactory = "kafkaListenerContainerFactory")
    public void onDividend(String value, Acknowledgment ack) {
        try {
            JsonNode root = om.readTree(value);
            String op = root.path("payload").path("op").asText("");
            if (!op.equals("c") && !op.equals("u")) { ack.acknowledge(); return; }

            JsonNode a = root.path("payload").path("after");
            if (a.isMissingNode() || a.isNull()) { ack.acknowledge(); return; }

            Long userId     = a.path("userId").isNull() ? null : a.path("userId").asLong();
            if (userId == null || userId <= 0) { ack.acknowledge(); return; }

            long dividendId = a.path("dividendId").asLong();
            String stockId  = a.path("stockId").asText(null);
            String amount   = a.path("cashDividend").isNull() ? null : a.path("cashDividend").asText(); // 주당 배당
            String rate     = a.path("dividendRate").isNull() ? null : a.path("dividendRate").asText();
            String payDate  = a.path("dividendDate").isNull() ? null : a.path("dividendDate").asText(); // 'YYYY-MM-DD'

            // 지급일=오늘(KST)만 알림 생성
            if (payDate == null || !LocalDate.parse(payDate).isEqual(LocalDate.now(KST))) {
                ack.acknowledge();
                return;
            }

            // Title/Message (English)
            String title = "Dividend Paid: " + (stockId != null ? stockId : "N/A");
            String message = String.format("%s  %s  dividend  %s per share",
                    payDate != null ? payDate : "",
                    stockId != null ? stockId : "N/A",
                    amount != null ? amount : "0");

            // 멱등키: div:<dividendId>:<userId>
            int n = jdbc.update("""
                INSERT IGNORE INTO notify.notification_event
                  (userId, type, title, message, data, dedupKey, createdAt)
                VALUES
                  (?, 'DIVIDEND', ?, ?,
                   JSON_OBJECT(
                     'dividendId', ?,
                     'stockId', ?,
                     'dividendDate', ?,
                     'cashDividend', ?,
                     'dividendRate', ?
                   ),
                   CONCAT('div:', ?, ':', ?),
                   NOW())
            """,
            userId, title, message,
            dividendId, stockId, payDate, amount, rate,
            dividendId, userId);

            log.info("DIVIDEND notify inserted userId={} dividendId={} rows={}", userId, dividendId, n);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Dividend consume failed: {}", e.toString(), e);
            // ack 생략 → 재처리
        }
    }
}
