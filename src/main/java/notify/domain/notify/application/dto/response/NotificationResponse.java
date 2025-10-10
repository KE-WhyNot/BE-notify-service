package notify.domain.notify.application.dto.response;

import notify.domain.notify.domain.entity.Notification;
import notify.domain.notify.domain.entity.NotificationType;

import java.time.Instant;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String message,
        Integer rank,
        String stockId,
        String stockName,
        Boolean isBuy,
        Integer quantity,
        Long price,
        String orderId,
        Instant filledAt,
        Long dividendAmount,
        LocalDateTime paymentDate,
        String data,
        String dedupKey,
        Instant createdAt,
        boolean read
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getRank(),
                n.getStockId(),
                n.getStockName(),
                n.getIsBuy(),
                n.getQuantity(),
                n.getPrice(),
                n.getOrderId(),
                n.getFilledAt(),
                n.getDividendAmount(),
                n.getPaymentDate(),
                n.getData(),
                n.getDedupKey(),
                n.getCreatedAt(),
                n.isRead()
        );
    }
}