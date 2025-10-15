package notify.domain.ranking.application.dto.response;

/**
 * 내 랭킹 응답 DTO
 */
public record MyRankingResponse(boolean inTop10, Integer rank, Double profitRate) {}
