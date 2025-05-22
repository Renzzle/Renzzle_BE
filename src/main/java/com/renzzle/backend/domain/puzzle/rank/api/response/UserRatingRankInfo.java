package com.renzzle.backend.domain.puzzle.rank.api.response;

import lombok.Builder;

@Builder
public record UserRatingRankInfo(
        int rank,
        String nickname,
        double rating
) {
}
