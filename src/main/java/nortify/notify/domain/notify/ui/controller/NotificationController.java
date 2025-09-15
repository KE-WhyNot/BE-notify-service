package nortify.notify.domain.notify.ui.controller;



import lombok.RequiredArgsConstructor;
import nortify.notify.domain.notify.application.dto.NotificationListResponse;
import nortify.notify.domain.notify.domain.service.NotificationCommandService;
import nortify.notify.domain.notify.domain.service.NotificationQueryService;
import nortify.notify.global.common.BaseResponse;
import nortify.notify.global.swagger.BaseApi;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static nortify.notify.global.exception.code.status.SuccessCode.OK;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController implements BaseApi {

    private final NotificationQueryService queryService;
    private final NotificationCommandService commandService;

    //전체 알림 조회
    @GetMapping
    public ResponseEntity<BaseResponse<NotificationListResponse>> list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String userId = (String) authentication.getPrincipal(); // GatewayAuthFilter가 세팅
        var body = queryService.findAll(userId, page, size);
        return BaseResponse.success(OK, body);
    }
    //읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<BaseResponse<Void>> markRead(
            Authentication authentication,
            @PathVariable Long notificationId
    ) {
        String userId = (String) authentication.getPrincipal();
        commandService.markRead(userId, notificationId);
        return BaseResponse.success(OK);
    }
    //전체 읽음 처리
    @PatchMapping("/read-all")
    public ResponseEntity<BaseResponse<Integer>> markAllRead(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        int updated = commandService.markAllRead(userId);
        return BaseResponse.success(OK, updated);
    }
}
