package notify.domain.notify.domain.service;

import lombok.RequiredArgsConstructor;
import notify.domain.notify.application.dto.response.NotificationResponse;
import notify.domain.notify.domain.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {
    private final NotificationRepository repo;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(String userId, Pageable pageable) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                   .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String userId) {
        return repo.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public int markRead(String userId, Long id) { 
        return repo.markRead(userId, id); 
    }

    @Transactional
    public int markAllRead(String userId) { 
        return repo.markAllRead(userId); 
    }
}