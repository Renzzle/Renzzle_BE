package com.renzzle.backend.domain.puzzle.training.api.response;

import lombok.Builder;

@Builder
public record GetTrainingPuzzleForAdminResponse(
        long id,
        String boardStatus,
        String answer,
        int depth,
        String winColor,
        int trainingIndex,
        boolean isSolved
) { }
