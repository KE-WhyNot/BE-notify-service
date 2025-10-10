package notify.domain.notify.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import notify.domain.notify.domain.entity.Notification;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class NotificationResponse {
    private Long notificationId;
    private Long userId;
    private String type;
    private String title;
    private String message;
    private String data;
    private String dedupKey;
    private Instant createdAt;
    private Instant readAt;
    private boolean read;

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
            n.getNotificationId(), n.getUserId(), n.getType().name(), n.getTitle(), n.getMessage(),
            n.getData(), n.getDedupKey(), n.getCreatedAt(), n.getReadAt(), n.isRead()
        );
    }
}