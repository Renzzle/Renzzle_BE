package com.renzzle.backend.domain.puzzle.rank.api.response;

import lombok.Builder;

@Builder
public record RankArchive(
        String boardStatus,
        String winColor,
        String answer,
        boolean isSolved
) {
}
