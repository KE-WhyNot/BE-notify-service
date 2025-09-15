package nortify.notify.domain.notify.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter @NoArgsConstructor
public class TradeNotificationRequest {
    @NotBlank
    private String userId;
    private String stockCode;
    @NotBlank
    private String stockName;
    @NotBlank
    private String side;      // BUY/SELL
    @NotNull
    @Positive
    private Integer quantity;
    @NotNull
    @Positive
    private Long price;
    @NotBlank
    private String orderId;
    @NotNull
    private Instant filledAt;
    private String message;
}
