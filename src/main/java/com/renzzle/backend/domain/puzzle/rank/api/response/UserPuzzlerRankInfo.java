package com.renzzle.backend.domain.puzzle.rank.api.response;

import lombok.Builder;

@Builder
public record UserPuzzlerRankInfo(
        int rank,
        String nickname,
        double score
) {
}
