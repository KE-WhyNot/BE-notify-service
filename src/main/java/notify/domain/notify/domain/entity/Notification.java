package notify.domain.notify.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_event")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificationId")
    private Long id;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "userId")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "readAt")
    private java.time.Instant readAt;

    @Column(name = "data", columnDefinition = "JSON")
    private String data;

    @Column(name = "dedupKey", length = 128)
    private String dedupKey;

    public boolean isRead() {
        return readAt != null;
    }

    public void markRead() {
        this.readAt = java.time.Instant.now();
    }
}
