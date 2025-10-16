package notify.domain.ranking.ui.controller;

import lombok.RequiredArgsConstructor;
import notify.domain.ranking.application.dto.response.MyRankingResponse;
import notify.domain.ranking.application.dto.response.RankingResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final JdbcTemplate jdbc;

    @GetMapping("/top10")
    public List<RankingResponse> top10() {
        return jdbc.query("""
                SELECT user_id, rank_no, profit_rate
                  FROM notify.ranking_top10
                 WHERE in_top10 = 1
                 ORDER BY rank_no ASC
                 LIMIT 10
            """, (rs, i) -> new RankingResponse(
                rs.getString("user_id"),
                rs.getInt("rank_no"),
                rs.getDouble("profit_rate")
        ));
    }

    @GetMapping("/myrank")
    public MyRankingResponse myRank(@RequestHeader("X-User-Id") String userId) {
        List<RankingResponse> rows = jdbc.query("""
                SELECT user_id, rank_no, profit_rate
                  FROM notify.ranking_top10
                 WHERE user_id = ?
                   AND in_top10 = 1
                 LIMIT 1
            """, (rs, i) -> new RankingResponse(
                rs.getString("user_id"),
                rs.getInt("rank_no"),
                rs.getDouble("profit_rate")
        ), userId);

        if (rows.isEmpty()) {
            return new MyRankingResponse(false, null, null);
        }
        var r = rows.get(0);
        return new MyRankingResponse(true, r.rankNo(), r.profitRate());
    }
}
