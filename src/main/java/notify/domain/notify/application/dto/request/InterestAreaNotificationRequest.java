package notify.domain.notify.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InterestAreaNotificationRequest(
        @NotBlank
        String userId,
        @NotBlank
        String topic,
        @NotBlank
        String message
) {}