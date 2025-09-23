package notify.domain.notify.ui.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import notify.domain.notify.application.dto.request.DividendNotificationRequest;
import notify.domain.notify.application.dto.request.RankingEnteredRequest;
import notify.domain.notify.application.dto.request.TradeNotificationRequest;
import notify.domain.notify.application.usecase.NotificationUseCase;
import notify.global.common.BaseResponse;
import notify.global.swagger.BaseApi;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static notify.global.exception.code.status.SuccessCode.CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/notifications")
public class InternalNotificationController implements BaseApi {

    private final NotificationUseCase notificationUseCase;

    @PostMapping("/ranking-entered")
    public BaseResponse<Void> rankingEntered(
            Authentication authentication,
            @Valid @RequestBody RankingEnteredRequest req
    ) {
        String userId = authentication.getName();
        notificationUseCase.createRankingNotification(req);
        return BaseResponse.success(CREATED).getBody();
    }

    @PostMapping("/trade")
    public BaseResponse<Void> trade(
            Authentication authentication,
            @Valid @RequestBody TradeNotificationRequest req
    ) {
        String userId = authentication.getName();
        notificationUseCase.createTradeNotification(req);
        return BaseResponse.success(CREATED).getBody();
    }

    @PostMapping("/dividend")
    public BaseResponse<Void> dividend(
            Authentication authentication,
            @Valid @RequestBody DividendNotificationRequest req
    ) {
        String userId = authentication.getName();
        notificationUseCase.createDividendNotification(req);
        return BaseResponse.success(CREATED).getBody();
    }

}
