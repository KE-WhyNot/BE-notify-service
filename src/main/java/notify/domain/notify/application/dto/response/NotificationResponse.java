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
        String data,
        String dedupKey,
        Instant createdAt,
        Instant readAt,
        boolean read
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getData(),
                n.getDedupKey(),
                n.getCreatedAt(),
                n.getReadAt(),
                n.isRead()
        );
    }
}