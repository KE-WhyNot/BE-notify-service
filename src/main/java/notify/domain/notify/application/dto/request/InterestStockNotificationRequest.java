package notify.domain.notify.application.dto.request;

import jakarta.validation.constraints.NotBlank;


public record InterestStockNotificationRequest(
        @NotBlank
        String userId,
        String stockCode,
        @NotBlank
        String stockName,
        Integer quantity,
        Long price,
        String side,          // BUY/SELL
        @NotBlank
        String message
) {}