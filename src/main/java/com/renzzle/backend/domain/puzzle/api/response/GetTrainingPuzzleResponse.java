package com.renzzle.backend.domain.puzzle.api.response;

import lombok.Builder;

@Builder
public record GetTrainingPuzzleResponse(
        long id,
        String boardStatus,
        int depth,
        String winColor,
        boolean isSolved
) { }
