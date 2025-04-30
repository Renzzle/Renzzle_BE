package com.renzzle.backend.domain.puzzle.rank.api.response;

import lombok.Builder;
import java.util.List;

@Builder
public record GetRankingResponse(
        List<UserRankInfo> top100,
        UserRankInfo myRank
) {
}
