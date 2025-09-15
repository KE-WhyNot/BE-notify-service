package nortify.notify.domain.notify.domain.service;

import lombok.RequiredArgsConstructor;
import nortify.notify.domain.notify.application.dto.NotificationListResponse;
import nortify.notify.domain.notify.application.dto.NotificationResponse;

import nortify.notify.domain.notify.domain.repository.NotificationRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {
    private final NotificationRepository repo;

    public NotificationListResponse findAll(String userId, int page, int size) {
        var mapped = repo
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(NotificationResponse::from);
        long unread = repo.countByUserIdAndReadIsFalse(userId);
        return NotificationListResponse.of(mapped.getContent(), mapped.isLast(), unread);
    }
}