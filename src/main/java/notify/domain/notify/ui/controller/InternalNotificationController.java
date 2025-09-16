package notify.domain.notify.ui.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import notify.domain.notify.application.dto.request.DividendNotificationRequest;
import notify.domain.notify.application.dto.request.InterestAreaNotificationRequest;
import notify.domain.notify.application.dto.request.InterestStockNotificationRequest;
import notify.domain.notify.application.dto.request.RankingEnteredRequest;
import notify.domain.notify.application.dto.request.TradeNotificationRequest;
import notify.domain.notify.usecase.NotificationUseCase;
import notify.global.common.BaseResponse;
import notify.global.swagger.BaseApi;
import org.springframework.web.bind.annotation.*;

import static notify.global.exception.code.status.SuccessCode.CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/notifications")
public class InternalNotificationController implements BaseApi {

    private final NotificationUseCase notificationUseCase;

    @PostMapping("/ranking-entered")
    public BaseResponse<Void> rankingEntered(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody RankingEnteredRequest req
    ) {
        notificationUseCase.createRankingNotification(req);
        return BaseResponse.success(CREATED).getBody();
    }

    @PostMapping("/trade")
    public BaseResponse<Void> trade(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody TradeNotificationRequest req
    ) {
        notificationUseCase.createTradeNotification(req);
        return BaseResponse.success(CREATED).getBody();
    }

    @PostMapping("/interest-stock")
    public BaseResponse<Void> interestStock(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody InterestStockNotificationRequest req
    ) {
        notificationUseCase.createInterestStockNotification(req);
        return BaseResponse.success(CREATED).getBody();
    }

    @PostMapping("/dividend")
    public BaseResponse<Void> dividend(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody DividendNotificationRequest req
    ) {
        notificationUseCase.createDividendNotification(req);
        return BaseResponse.success(CREATED).getBody();
    }

    @PostMapping("/interest-area")
    public BaseResponse<Void> interestArea(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody InterestAreaNotificationRequest req
    ) {
        notificationUseCase.createInterestAreaNotification(req);
        return BaseResponse.success(CREATED).getBody();
    }
}
