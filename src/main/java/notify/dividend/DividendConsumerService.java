package notify.dividend;

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
public class DividendConsumerService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper om = new ObjectMapper();

    /** Debezium topic: finance.finance.dividendInfo -> notify.dividend_notice UPSERT */
    @KafkaListener(topics = "finance.finance.dividendInfo", containerFactory = "kafkaListenerContainerFactory")
    public void onDividend(String value, Acknowledgment ack) {
        try {
            JsonNode root = om.readTree(value);
            String op = root.path("payload").path("op").asText("");
            if (!op.equals("c") && !op.equals("u")) { ack.acknowledge(); return; }

            JsonNode a = root.path("payload").path("after");
            if (a.isMissingNode() || a.isNull()) { ack.acknowledge(); return; }

            long dividendId     = a.path("dividendId").asLong();
            String stockId      = a.path("stockId").asText(null);
            Long sectorId       = a.path("sectorId").isNull() ? null : a.path("sectorId").asLong();
            String recordDate   = a.path("recordDate").asText(null);
            String cashDividend = a.path("cashDividend").isNull() ? null : a.path("cashDividend").asText();
            String dividendRate = a.path("dividendRate").isNull() ? null : a.path("dividendRate").asText();
            String dividendDate = a.path("dividendDate").isNull() ? null : a.path("dividendDate").asText();

            int n = jdbc.update("""
                INSERT INTO notify.dividend_notice
                  (dividendId, stockId, sectorId, recordDate, cashDividend, dividendRate, dividendDate, createdAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE
                  stockId=VALUES(stockId), sectorId=VALUES(sectorId),
                  recordDate=VALUES(recordDate), cashDividend=VALUES(cashDividend),
                  dividendRate=VALUES(dividendRate), dividendDate=VALUES(dividendDate)
            """, dividendId, stockId, sectorId, recordDate, cashDividend, dividendRate, dividendDate);
            log.info("dividend_notice upserted id={} rows={}", dividendId, n);

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Dividend consume failed: {}", e.toString(), e);
            // no ack -> reprocess
        }
    }
}
