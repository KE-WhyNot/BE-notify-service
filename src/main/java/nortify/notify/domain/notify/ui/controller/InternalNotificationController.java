package nortify.notify.domain.notify.ui.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nortify.notify.domain.notify.application.dto.request.*;
import nortify.notify.domain.notify.domain.service.NotificationCommandService;
import nortify.notify.global.common.BaseResponse;
import nortify.notify.global.swagger.BaseApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static nortify.notify.global.exception.code.status.SuccessCode.CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/notifications")
public class InternalNotificationController implements BaseApi {

    private final NotificationCommandService commandService;

    @PostMapping("/ranking-entered")
    public ResponseEntity<BaseResponse<Void>> rankingEntered(
            @Valid @RequestBody RankingEnteredRequest req
    ) {
        commandService.createRankingNotification(req);
        return BaseResponse.success(CREATED);
    }

    @PostMapping("/trade")
    public ResponseEntity<BaseResponse<Void>> trade(
            @Valid @RequestBody TradeNotificationRequest req
    ) {
        commandService.createTradeNotification(req);
        return BaseResponse.success(CREATED);
    }

    @PostMapping("/interest-stock")
    public ResponseEntity<BaseResponse<Void>> interestStock(
            @Valid @RequestBody InterestStockNotificationRequest req
    ) {
        commandService.createInterestStockNotification(req);
        return BaseResponse.success(CREATED);
    }

    @PostMapping("/dividend")
    public ResponseEntity<BaseResponse<Void>> dividend(
            @Valid @RequestBody DividendNotificationRequest req
    ) {
        commandService.createDividendNotification(req);
        return BaseResponse.success(CREATED);
    }

    @PostMapping("/interest-area")
    public ResponseEntity<BaseResponse<Void>> interestArea(
            @Valid @RequestBody InterestAreaNotificationRequest req
    ) {
        commandService.createInterestAreaNotification(req);
        return BaseResponse.success(CREATED);
    }
}
