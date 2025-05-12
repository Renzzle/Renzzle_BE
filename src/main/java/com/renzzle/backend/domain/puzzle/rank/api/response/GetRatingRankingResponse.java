package com.renzzle.backend.domain.puzzle.rank.api.response;

import lombok.Builder;
import java.util.List;

@Builder
public record GetRatingRankingResponse(
        List<UserRatingRankInfo> top100,
        UserRatingRankInfo myRatingRank
) {
}
