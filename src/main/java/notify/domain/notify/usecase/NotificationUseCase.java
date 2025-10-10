package notify.domain.notify.usecase;

import lombok.RequiredArgsConstructor;
import notify.domain.notify.application.dto.response.NotificationResponse;
import notify.domain.notify.domain.service.NotificationQueryService;
import notify.global.exception.RestApiException;
import notify.global.exception.code.status.NotificationErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationUseCase {
    private final NotificationQueryService queryService;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Long userId, Pageable pageable) {
        if (userId == null || userId <= 0) {
            throw new RestApiException(NotificationErrorCode.INVALID_USER_ID);
        }
        return queryService.list(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        if (userId == null || userId <= 0) {
            throw new RestApiException(NotificationErrorCode.INVALID_USER_ID);
        }
        return queryService.unreadCount(userId);
    }

    public void markRead(Long userId, Long id) {
        if (userId == null || userId <= 0) {
            throw new RestApiException(NotificationErrorCode.INVALID_USER_ID);
        }
        int updated = queryService.markRead(userId, id);
        if (updated == 0) {
            throw new RestApiException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }

    public int markAllRead(Long userId) {
        if (userId == null || userId <= 0) {
            throw new RestApiException(NotificationErrorCode.INVALID_USER_ID);
        }
        return queryService.markAllRead(userId);
    }
}
