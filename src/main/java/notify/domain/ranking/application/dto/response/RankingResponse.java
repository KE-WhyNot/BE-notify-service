package notify.domain.ranking.application.dto.response;

/**
 * 랭킹 응답 DTO
 */
public record RankingResponse(String userId, int rank, double profitRate) {}
