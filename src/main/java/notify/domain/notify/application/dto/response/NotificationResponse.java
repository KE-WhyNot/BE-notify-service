package notify.domain.notify.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import notify.domain.notify.domain.entity.Notification;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String message;
    private String data;
    private Instant createdAt;
    private boolean read;

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
            n.getId(), n.getType().name(), n.getTitle(), n.getMessage(),
            n.getData(), n.getCreatedAt(), n.isRead()
        );
    }
}