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

    private static LocalDate parseDebeziumDate(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) return null;
        // 숫자 형태 (에포크 일수)
        if (n.isNumber() || n.asText().matches("^\\d+$")) {
            return LocalDate.ofEpochDay(n.asLong());
        }
        // 문자열 날짜 형태 'YYYY-MM-DD'
        String s = n.asText();
        if (s.length() >= 10) {
            return LocalDate.parse(s.substring(0, 10));
        }
        return null;
    }

    @KafkaListener(topics = "finance.finance.dividendInfo", containerFactory = "kafkaListenerContainerFactory")
    public void onDividend(String value, Acknowledgment ack) {
        try {
            JsonNode root = om.readTree(value);
            String op = root.path("payload").path("op").asText("");
            if (!op.equals("c") && !op.equals("u")) { ack.acknowledge(); return; }

            JsonNode a = root.path("payload").path("after");
            if (a.isMissingNode() || a.isNull()) { ack.acknowledge(); return; }

            String userId = a.path("userId").isNull() ? null : a.path("userId").asText();
            if (userId == null || userId.trim().isEmpty()) { ack.acknowledge(); return; }

            long dividendId = a.path("dividendId").asLong();
            String stockId = a.path("stockId").asText(null);
            Long sectorId = a.path("sectorId").isNull() ? null : a.path("sectorId").asLong();
            String amount = a.path("cashDividend").isNull() ? null : a.path("cashDividend").asText();
            String rate = a.path("dividendRate").isNull() ? null : a.path("dividendRate").asText();

            // 날짜 파싱
            LocalDate recordDate = parseDebeziumDate(a.path("recordDate"));
            LocalDate dividendDate = parseDebeziumDate(a.path("dividendDate"));

            // notify.dividend_notice에 UPSERT (지급일과 관계없이 모든 데이터 저장)
            int n = jdbc.update("""
                INSERT INTO notify.dividend_notice
                  (dividendId, userId, stockId, sectorId, recordDate, cashDividend, dividendRate, dividendDate, createdAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE
                  stockId=VALUES(stockId),
                  sectorId=VALUES(sectorId),
                  recordDate=VALUES(recordDate),
                  cashDividend=VALUES(cashDividend),
                  dividendRate=VALUES(dividendRate),
                  dividendDate=VALUES(dividendDate),
                  updatedAt=CURRENT_TIMESTAMP
            """,
            dividendId, userId, stockId, sectorId,
            java.sql.Date.valueOf(recordDate),   // recordDate
            amount,                              // cashDividend
            rate,                                // dividendRate
            java.sql.Date.valueOf(dividendDate)  // dividendDate
            );

            log.info("DIVIDEND notice upserted userId={} dividendId={} rows={}", userId, dividendId, n);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Dividend consume failed: {}", e.toString(), e);
            // ack 생략 → 재처리
        }
    }
}
