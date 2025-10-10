package notify.domain.notify.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(of = "notificationId")
@Entity
@Table(
    name = "notification_event",
    schema = "notify",
    indexes = {
        @Index(name = "idx_notification_event_user_createdAt", columnList = "userId, createdAt"),
        @Index(name = "uq_notification_event_dedupKey", columnList = "dedupKey", unique = true)
    }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificationId")
    private Long notificationId;

    @Column(name = "userId", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private NotificationType type;  // e.g. EXECUTION, DIVIDEND

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "data", columnDefinition = "JSON")
    private String data;

    @Column(name = "dedupKey", length = 128, unique = true)
    private String dedupKey;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "readAt")
    private Instant readAt;

    public boolean isRead() { return readAt != null; }
    public void markRead()  { this.readAt = Instant.now(); }
}
