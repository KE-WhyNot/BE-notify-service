package nortify.notify.domain.notify.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InterestStockNotificationRequest {
    @NotBlank
    private String userId;
    private String stockCode;
    @NotBlank
    private String stockName;
    private Integer quantity;
    private Long price;
    private String side;        // BUY/SELL
    @NotBlank
    private String message;
}