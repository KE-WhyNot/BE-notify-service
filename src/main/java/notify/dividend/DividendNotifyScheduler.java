package notify.dividend;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class DividendNotifyScheduler {

    private final JdbcTemplate jdbc;

    /** 매일 09:00 KST 배당일 알림(해당 종목 보유자에게만) */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void publishTodayDividend() {
        int rows = jdbc.update("""
            INSERT IGNORE INTO notify.notification_event
              (userId, type, title, message, data, dedupKey, createdAt)
            SELECT
              us.userId,
              'DIVIDEND',
              CONCAT('배당 지급: ', d.stockId),
              CONCAT(DATE_FORMAT(d.dividendDate,'%Y-%m-%d'),' ', d.stockId,' 배당금 ', IFNULL(d.cashDividend,0)),
              JSON_OBJECT(
                'dividendId', d.dividendId,
                'stockId', d.stockId,
                'dividendDate', d.dividendDate,
                'cashDividend', d.cashDividend,
                'dividendRate', d.dividendRate
              ),
              CONCAT('div:', d.dividendId),
              NOW()
            FROM notify.dividend_notice d
            JOIN finance.userstock us ON us.stockId = d.stockId AND us.holdingQuantity > 0
            WHERE d.dividendDate = CURRENT_DATE()
        """);
        log.info("Dividend notifications created: {}", rows);
    }
}
