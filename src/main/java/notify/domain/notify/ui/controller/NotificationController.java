package notify.domain.notify.ui.controller;

import lombok.RequiredArgsConstructor;
import notify.domain.notify.application.dto.response.NotificationResponse;
import notify.domain.notify.usecase.NotificationUseCase;
import notify.global.common.BaseResponse;
import notify.global.swagger.BaseApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

import static notify.global.exception.code.status.SuccessCode.OK;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController implements BaseApi {
    private final NotificationUseCase useCase;

    @GetMapping
    public BaseResponse<Page<NotificationResponse>> list(
            Principal principal,
            @PageableDefault(size = 20) Pageable pageable) {

        String userId = principal.getName(); // ← 여기서 가져옴

        Pageable fixedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<NotificationResponse> body = useCase.list(userId, fixedPageable);
        return BaseResponse.success(OK, body).getBody();
    }

    @GetMapping("/unread-count")
    public BaseResponse<Map<String, Long>> unreadCount(Principal principal) {
        String userId = principal.getName(); // ← 여기서 가져옴
        Map<String, Long> body = Map.of("count", useCase.unreadCount(userId));
        return BaseResponse.success(OK, body).getBody();
    }

    @PostMapping("/{id}/read")
    public BaseResponse<Void> markRead(Principal principal, @PathVariable Long id) {
        String userId = principal.getName(); // ← 여기서 가져옴
        useCase.markRead(userId, id);
        return BaseResponse.success(OK).getBody();
    }

    @PostMapping("/read-all")
    public BaseResponse<Map<String, Integer>> markAllRead(Principal principal) {
        String userId = principal.getName(); // ← 여기서 가져옴
        Map<String, Integer> body = Map.of("updated", useCase.markAllRead(userId));
        return BaseResponse.success(OK, body).getBody();
    }
}