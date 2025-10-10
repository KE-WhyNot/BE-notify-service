package notify.domain.notify.usecase;

import lombok.RequiredArgsConstructor;
import notify.domain.notify.application.dto.response.NotificationListResponse;
import notify.domain.notify.domain.service.NotificationCommandService;
import notify.domain.notify.domain.service.NotificationQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationUseCase {
    private final NotificationCommandService commandService;
    private final NotificationQueryService queryService;

    @Transactional(readOnly = true) // 조회 메서드는 readOnly로 오버라이드
    public NotificationListResponse listAll(Long userId, int page, int size) {
        return queryService.findAll(userId, page, size);
    }

    public void markRead(Long userId, Long notificationId) {
        commandService.markRead(userId, notificationId);
    }

    public int markAllRead(Long userId) {
        return commandService.markAllRead(userId);
    }

}
