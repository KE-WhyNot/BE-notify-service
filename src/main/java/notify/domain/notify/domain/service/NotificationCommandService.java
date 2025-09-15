package notify.domain.notify.domain.service;


import lombok.RequiredArgsConstructor;
import nortify.notify.domain.notify.application.dto.*;
import nortify.notify.domain.notify.application.dto.request.*;
import notify.domain.notify.application.dto.request.*;
import notify.domain.notify.domain.entity.Notification;
import notify.domain.notify.domain.entity.NotificationType;
import notify.domain.notify.domain.entity.TradeSide;
import notify.domain.notify.domain.repository.NotificationRepository;
import notify.global.exception.RestApiException;
import notify.global.exception.code.status.GlobalErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationCommandService {
    private final NotificationRepository repo;

    //읽음 처리
    @Transactional
    public void markRead(String userId, Long notificationId) {
        int updated = repo.markRead(userId, notificationId);
        if (updated == 0) throw new RestApiException(GlobalErrorCode.NOT_FOUND);
    }

    @Transactional
    public int markAllRead(String userId) {
        return repo.markAllRead(userId);
    }

    //랭킹 (Top10 진입)
    @Transactional
    public Long createRankingNotification(RankingEnteredRequest req) {
        String msg = (req.getMessage()!=null && !req.getMessage().isBlank())
                ? req.getMessage()
                : "모의투자 수익률 TOP 10 진입";
        Notification n = Notification.builder()
                .userId(req.getUserId())
                .type(NotificationType.RANKING)
                .rank(req.getRank())
                .message(msg)
                .build();
        return repo.save(n).getId();
    }

    //TRADE
    @Transactional
    public Long createTradeNotification(TradeNotificationRequest r) {
        String msg = (r.getMessage()!=null && !r.getMessage().isBlank())
                ? r.getMessage()
                : (("BUY".equalsIgnoreCase(r.getSide()) ? "매수" : "매도") +
                " 체결: " + r.getStockName() + " " + r.getQuantity() + "주 @ " + r.getPrice() + "원");

        Notification n = Notification.builder()
                .userId(r.getUserId())
                .type(NotificationType.TRADE)
                .stockCode(r.getStockCode())
                .stockName(r.getStockName())
                .side("BUY".equalsIgnoreCase(r.getSide()) ? TradeSide.BUY : TradeSide.SELL)
                .quantity(r.getQuantity())
                .price(r.getPrice())
                .orderId(r.getOrderId())
                .filledAt(r.getFilledAt())
                .message(msg)
                .build();
        return repo.save(n).getId();
    }

    //INTERESTSTOCK
    @Transactional
    public Long createInterestStockNotification(InterestStockNotificationRequest r) {
        Notification n = Notification.builder()
                .userId(r.getUserId())
                .type(NotificationType.INTERESTSTOCK)
                .stockCode(r.getStockCode())
                .stockName(r.getStockName())
                .quantity(r.getQuantity())
                .price(r.getPrice())
                .side(r.getSide()!=null && r.getSide().equalsIgnoreCase("BUY") ? TradeSide.BUY :
                        (r.getSide()!=null && r.getSide().equalsIgnoreCase("SELL") ? TradeSide.SELL : null))
                .message(r.getMessage())
                .build();
        return repo.save(n).getId();
    }

    //DIVIDEND
    @Transactional
    public Long createDividendNotification(DividendNotificationRequest r) {
        String msg = (r.getMessage()!=null && !r.getMessage().isBlank())
                ? r.getMessage()
                : ( "배당금 지급: " + r.getStockName() + " " + r.getDividendAmount() + "원 입금" );

        Notification n = Notification.builder()
                .userId(r.getUserId())
                .type(NotificationType.DIVIDEND)
                .stockCode(r.getStockCode())
                .stockName(r.getStockName())
                // 지급 총액
                .dividendAmount(r.getDividendAmount())
                // 지급 일자
                .paymentDate(r.getPaymentDate())
                .message(msg)
                .build();

        return repo.save(n).getId();
    }

    // INTERESTAREA
    @Transactional
    public Long createInterestAreaNotification(InterestAreaNotificationRequest r) {
        Notification n = Notification.builder()
                .userId(r.getUserId())
                .type(NotificationType.INTERESTAREA)
                .topic(r.getTopic())
                .message(r.getMessage())
                .build();
        return repo.save(n).getId();
    }
}
