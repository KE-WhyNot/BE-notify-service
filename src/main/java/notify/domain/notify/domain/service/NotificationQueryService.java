package notify.domain.notify.domain.service;

import lombok.RequiredArgsConstructor;
import notify.domain.notify.application.dto.response.NotificationListResponse;
import notify.domain.notify.application.dto.response.NotificationResponse;

import notify.domain.notify.domain.repository.NotificationRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {
    private final NotificationRepository repo;

    public NotificationListResponse findAll(Long userId, int page, int size) {
        var mapped = repo
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(NotificationResponse::from);
        long unread = repo.countByUserIdAndReadAtIsNull(userId);
        return NotificationListResponse.of(mapped.getContent(), mapped.isLast(), unread);
    }
}