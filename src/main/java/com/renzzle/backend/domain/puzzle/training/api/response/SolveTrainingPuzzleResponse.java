package com.renzzle.backend.domain.puzzle.training.api.response;

import lombok.Builder;

@Builder
public record SolveTrainingPuzzleResponse(
        int reward
) { }
