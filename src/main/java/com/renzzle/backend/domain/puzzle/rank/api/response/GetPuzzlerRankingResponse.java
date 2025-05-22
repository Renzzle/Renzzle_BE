package com.renzzle.backend.domain.puzzle.rank.api.response;

import lombok.Builder;

import java.util.List;

@Builder
public record GetPuzzlerRankingResponse(
        List<UserPuzzlerRankInfo> top100,
        UserPuzzlerRankInfo myPuzzlerRank
) {
}
