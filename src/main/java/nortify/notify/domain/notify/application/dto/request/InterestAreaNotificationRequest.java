package nortify.notify.domain.notify.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InterestAreaNotificationRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String topic;
    @NotBlank
    private String message;
}