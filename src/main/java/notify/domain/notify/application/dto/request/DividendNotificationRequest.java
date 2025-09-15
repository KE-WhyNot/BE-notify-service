package notify.domain.notify.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class DividendNotificationRequest {
    @NotBlank
    private String userId;
    private String stockCode;
    @NotBlank
    private String stockName;
    @NotNull
    @Positive
    private Integer dividendAmount;
    @NotNull
    private LocalDate paymentDate;
    private String message;
}