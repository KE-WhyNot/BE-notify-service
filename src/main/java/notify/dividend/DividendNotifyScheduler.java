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

    /** 매일 09:00 KST, 지급일=오늘인 건을 알림 생성(이미 있으면 IGNORE) */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void publishTodayDividend() {
        int rows = jdbc.update("""
            INSERT IGNORE INTO notify.notification_event
              (userId, type, title, message, data, dedupKey, createdAt)
            SELECT
              dn.userId,
              'DIVIDEND',
              CONCAT('배당 지급: ', dn.stockId),
              CONCAT(dn.stockId, '  배당금  ', IFNULL(dn.cashDividend,0), '원'),
              JSON_OBJECT(
                'dividendId', dn.dividendId,
                'stockId', dn.stockId,
                'recordDate', dn.recordDate,
                'dividendDate', dn.dividendDate,
                'cashDividend', dn.cashDividend,
                'dividendRate', dn.dividendRate
              ),
              CONCAT('div:', dn.dividendId, ':', dn.userId),
              NOW()
            FROM notify.dividend_notice dn
            WHERE dn.dividendDate = DATE(CONVERT_TZ(UTC_TIMESTAMP(), 'UTC', '+09:00'))
        """);
        log.info("Dividend notifications created by scheduler: {}", rows);
    }
}
