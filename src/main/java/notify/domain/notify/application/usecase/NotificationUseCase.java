package notify.domain.notify.application.usecase;

import lombok.RequiredArgsConstructor;
import notify.domain.notify.application.dto.request.DividendNotificationRequest;
import notify.domain.notify.application.dto.request.RankingEnteredRequest;
import notify.domain.notify.application.dto.request.TradeNotificationRequest;
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

    public NotificationListResponse listAll(String userId, int page, int size) {
        return queryService.findAll(userId, page, size);
    }

    public void markRead(String userId, Long notificationId) {
        commandService.markRead(userId, notificationId);
    }

    public int markAllRead(String userId) {
        return commandService.markAllRead(userId);
    }

    public Long createRankingNotification(RankingEnteredRequest req) {
        return commandService.createRankingNotification(req);
    }

    public Long createTradeNotification(TradeNotificationRequest req) {
        return commandService.createTradeNotification(req);
    }


    public Long createDividendNotification(DividendNotificationRequest req) {
        return commandService.createDividendNotification(req);
    }

}
