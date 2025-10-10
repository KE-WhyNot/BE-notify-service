package notify.domain.notify.ui.controller;

import lombok.RequiredArgsConstructor;
import notify.domain.notify.application.dto.response.NotificationResponse;
import notify.domain.notify.usecase.NotificationUseCase;
import notify.global.common.BaseResponse;
import notify.global.swagger.BaseApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static notify.global.exception.code.status.SuccessCode.OK;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController implements BaseApi {
    private final NotificationUseCase useCase;

    @GetMapping
    public BaseResponse<Page<NotificationResponse>> list(
            @RequestHeader("X-USER-ID") Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<NotificationResponse> body = useCase.list(userId, pageable);
        return BaseResponse.success(OK, body).getBody();
    }

    @GetMapping("/unread-count")
    public BaseResponse<Map<String, Long>> unreadCount(@RequestHeader("X-USER-ID") Long userId) {
        Map<String, Long> body = Map.of("count", useCase.unreadCount(userId));
        return BaseResponse.success(OK, body).getBody();
    }

    @PostMapping("/{id}/read")
    public BaseResponse<Void> markRead(@RequestHeader("X-USER-ID") Long userId, @PathVariable Long id) {
        useCase.markRead(userId, id);
        return BaseResponse.success(OK).getBody();
    }

    @PostMapping("/read-all")
    public BaseResponse<Map<String, Integer>> markAllRead(@RequestHeader("X-USER-ID") Long userId) {
        Map<String, Integer> body = Map.of("updated", useCase.markAllRead(userId));
        return BaseResponse.success(OK, body).getBody();
    }
}