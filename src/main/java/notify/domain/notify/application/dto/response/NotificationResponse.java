package notify.domain.notify.application.dto.response;

import notify.domain.notify.domain.entity.Notification;
import notify.domain.notify.domain.entity.NotificationType;
import notify.domain.notify.domain.entity.TradeSide;

import java.time.Instant;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        Integer rank,
        String stockCode,
        String stockName,
        TradeSide side,
        Integer quantity,
        Long price,
        String orderId,
        Instant filledAt,
        Integer dividendAmount,
        LocalDateTime paymentDate,
        String message,
        Instant createdAt,
        boolean read
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getRank(),
                n.getStockCode(),
                n.getStockName(),
                n.getSide(),
                n.getQuantity(),
                n.getPrice(),
                n.getOrderId(),
                n.getFilledAt(),
                n.getDividendAmount(),
                n.getPaymentDate(),
                n.getMessage(),
                n.getCreatedAt(),
                n.isRead()
        );
    }
}