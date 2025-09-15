package nortify.notify.domain.notify.application.dto.request;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
public class RankingEnteredRequest {
    private String userId;
    private Integer rank;
    private String message;
}