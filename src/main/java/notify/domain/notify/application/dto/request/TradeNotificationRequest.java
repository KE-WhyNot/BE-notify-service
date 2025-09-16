package notify.domain.notify.application.dto.request;

import jakarta.validation.constraints.*;

public record TradeNotificationRequest(
        @NotNull
        Long userId,
        @NotBlank
        String stockCode,
        @NotBlank
        String stockName,
        @NotBlank
        String side,      // "BUY" / "SELL" 등
        @Positive
        int quantity,
        @Positive
        Long price,
        String message              // 선택
) {}