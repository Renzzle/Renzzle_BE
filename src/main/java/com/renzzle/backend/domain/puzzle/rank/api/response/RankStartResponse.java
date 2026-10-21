package com.renzzle.backend.domain.puzzle.rank.api.response;

import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import lombok.Builder;

@Builder
public record RankStartResponse(
        String boardStatus,
        String winColor,
        PuzzleType puzzleType,
        Long puzzleId
) {
}
