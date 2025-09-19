package notify.domain.notify.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record DividendNotificationRequest(
        @NotBlank
        String userId,
        String stockCode,
        @NotBlank
        String stockName,
        @NotNull @Positive
        Integer dividendAmount,
        @NotNull
        LocalDateTime paymentDate,
        String message
) {}