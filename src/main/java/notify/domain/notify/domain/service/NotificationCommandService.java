package notify.domain.notify.domain.service;

import lombok.RequiredArgsConstructor;
import notify.domain.notify.application.dto.request.DividendNotificationRequest;
import notify.domain.notify.application.dto.request.InterestAreaNotificationRequest;
import notify.domain.notify.application.dto.request.InterestStockNotificationRequest;
import notify.domain.notify.application.dto.request.RankingEnteredRequest;
import notify.domain.notify.application.dto.request.TradeNotificationRequest;
import notify.domain.notify.domain.entity.Notification;
import notify.domain.notify.domain.entity.NotificationType;
import notify.domain.notify.domain.entity.TradeSide;
import notify.domain.notify.domain.repository.NotificationRepository;
import notify.global.exception.RestApiException;
import notify.global.exception.code.status.GlobalErrorCode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationCommandService {
    private final NotificationRepository repo;

    //읽음 처리
    public void markRead(String userId, Long notificationId) {
        int updated = repo.markRead(userId, notificationId);
        if (updated == 0) throw new RestApiException(GlobalErrorCode.NOT_FOUND);
    }

    //전체 읽음 처리
    public int markAllRead(String userId) {
        return repo.markAllRead(userId);
    }

    //랭킹 (Top10 진입)
    public Long createRankingNotification(RankingEnteredRequest req) {
        String msg = (req.message()!=null && !req.message().isBlank())
                ? req.message()
                : "모의투자 수익률 TOP 10 진입";
        Notification n = Notification.builder()
                .userId(req.userId())
                .type(NotificationType.RANKING)
                .rank(req.rank())
                .message(msg)
                .build();
        return repo.save(n).getId();
    }

    //TRADE
    public Long createTradeNotification(TradeNotificationRequest r) {
        String msg = (r.message()!=null && !r.message().isBlank())
                ? r.message()
                : (("BUY".equalsIgnoreCase(r.side()) ? "매수" : "매도") +
                " 체결: " + r.stockName() + " " + r.quantity() + "주 @ " + r.price() + "원");

        Notification n = Notification.builder()
                .userId(String.valueOf(r.userId()))
                .type(NotificationType.TRADE)
                .stockCode(r.stockCode())
                .stockName(r.stockName())
                .side("BUY".equalsIgnoreCase(r.side()) ? TradeSide.BUY : TradeSide.SELL)
                .quantity(r.quantity())
                .price(r.price())
                .orderId(null)
                .filledAt(null)
                .message(msg)
                .build();
        return repo.save(n).getId();
    }

    //INTERESTSTOCK
    public Long createInterestStockNotification(InterestStockNotificationRequest r) {
        Notification n = Notification.builder()
                .userId(r.userId())
                .type(NotificationType.INTERESTSTOCK)
                .stockCode(r.stockCode())
                .stockName(r.stockName())
                .quantity(r.quantity())
                .price(r.price())
                .side(r.side()!=null && r.side().equalsIgnoreCase("BUY") ? TradeSide.BUY :
                        (r.side()!=null && r.side().equalsIgnoreCase("SELL") ? TradeSide.SELL : null))
                .message(r.message())
                .build();
        return repo.save(n).getId();
    }

    //DIVIDEND
    public Long createDividendNotification(DividendNotificationRequest r) {
        String msg = (r.message()!=null && !r.message().isBlank())
                ? r.message()
                : ( "배당금 지급: " + r.stockName() + " " + r.dividendAmount() + "원 입금" );

        Notification n = Notification.builder()
                .userId(r.userId())
                .type(NotificationType.DIVIDEND)
                .stockCode(r.stockCode())
                .stockName(r.stockName())
                // 지급 총액
                .dividendAmount(r.dividendAmount())
                // 지급 일자
                .paymentDate(r.paymentDate())
                .message(msg)
                .build();

        return repo.save(n).getId();
    }

    // INTERESTAREA
    public Long createInterestAreaNotification(InterestAreaNotificationRequest r) {
        Notification n = Notification.builder()
                .userId(r.userId())
                .type(NotificationType.INTERESTAREA)
                .topic(r.topic())
                .message(r.message())
                .build();
        return repo.save(n).getId();
    }
}
