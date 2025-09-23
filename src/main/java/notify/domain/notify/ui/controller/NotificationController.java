package notify.domain.notify.ui.controller;

import lombok.RequiredArgsConstructor;
import notify.domain.notify.application.dto.response.NotificationListResponse;
import notify.domain.notify.application.usecase.NotificationUseCase;
import notify.global.common.BaseResponse;
import notify.global.swagger.BaseApi;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static notify.global.exception.code.status.SuccessCode.OK;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController implements BaseApi {

    private final NotificationUseCase notificationUseCase;



    // 전체 알림 조회
    @GetMapping
    public BaseResponse<NotificationListResponse> list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String userId = authentication.getName();
        NotificationListResponse body = notificationUseCase.listAll(userId, page, size);
        return BaseResponse.success(OK, body).getBody();
    }

    // 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public BaseResponse<Void> markRead(
            Authentication authentication,
            @PathVariable Long notificationId
    ) {
        String userId = authentication.getName();
        notificationUseCase.markRead(userId, notificationId);
        return BaseResponse.success(OK).getBody();
    }

    // 전체 읽음 처리
    @PatchMapping("/read-all")
    public BaseResponse<Integer> markAllRead(
            Authentication authentication
    ) {
        String userId = authentication.getName();
        int updated = notificationUseCase.markAllRead(userId);
        return BaseResponse.success(OK, updated).getBody();
    }
}
