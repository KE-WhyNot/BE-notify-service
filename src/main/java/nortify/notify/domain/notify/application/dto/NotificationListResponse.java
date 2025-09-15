package nortify.notify.domain.notify.application.dto;

import java.util.List;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationListResponse {
    private String name;
    private List<NotificationResponse> notifications;
    private boolean last;
    private long unreadCount;

    public static NotificationListResponse of(List<NotificationResponse> list, boolean last, long unread) {
        return NotificationListResponse.builder()
                .name("알림 목록")
                .notifications(list)
                .last(last)
                .unreadCount(unread)
                .build();
    }
}