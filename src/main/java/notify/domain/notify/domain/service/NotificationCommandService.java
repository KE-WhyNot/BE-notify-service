package notify.domain.notify.domain.service;

import lombok.RequiredArgsConstructor;
import notify.domain.notify.domain.repository.NotificationRepository;
import notify.global.exception.RestApiException;
import notify.global.exception.code.status.GlobalErrorCode;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class NotificationCommandService {
    private final NotificationRepository repo;

    //읽음 처리
    public void markRead(Long userId, Long notificationId) {
        int updated = repo.markRead(userId, notificationId);
        if (updated == 0) throw new RestApiException(GlobalErrorCode.NOT_FOUND);
    }

    //전체 읽음 처리
    public int markAllRead(Long userId) {
        return repo.markAllRead(userId);
    }



}
