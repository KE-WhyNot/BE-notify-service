package nortify.notify.domain.notify.application.dto;

import lombok.*;
import nortify.notify.domain.notify.domain.entity.Notification;
import nortify.notify.domain.notify.domain.entity.NotificationType;
import nortify.notify.domain.notify.domain.entity.TradeSide;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long notificationId;
    private NotificationType type;
    private Integer rank;
    private String stockCode;
    private String stockName;
    private TradeSide side;
    private Integer quantity;
    private Long price;
    private String orderId;
    private Instant filledAt;
    private Integer dividendAmount;
    private LocalDate paymentDate;
    private String topic;
    private String message;
    private Instant createdAt;
    private boolean read;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getId())
                .type(n.getType())
                .rank(n.getRank())
                .stockCode(n.getStockCode())
                .stockName(n.getStockName())
                .side(n.getSide())
                .quantity(n.getQuantity())
                .price(n.getPrice())
                .orderId(n.getOrderId())
                .filledAt(n.getFilledAt())
                .dividendAmount(n.getDividendAmount())
                .paymentDate(n.getPaymentDate())
                .topic(n.getTopic())
                .message(n.getMessage())
                .createdAt(n.getCreatedAt())
                .read(n.isRead())
                .build();
    }
}
