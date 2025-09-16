package notify.domain.notify.application.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RankingEnteredRequest(
        @NotBlank
        String userId,
        @NotNull
        Integer rank,
        String message
) {}