package notify.domain.notify.application.dto.response;

import java.util.List;

public record NotificationListResponse(
        String name,
        List<NotificationResponse> notifications,
        boolean last,
        long unreadCount
) {
    public static NotificationListResponse of(List<NotificationResponse> list, boolean last, long unread) {
        return new NotificationListResponse(
                "알림 목록",
                list,
                last,
                unread
        );
    }
}