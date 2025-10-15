package notify.ranking;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RankItem {
    @JsonProperty("user_id")
    private String userId;
    
    private int rank;
    
    @JsonProperty("profit_rate")
    private double profitRate;
}
