package com.renzzle.backend.domain.puzzle.api.request;

import jakarta.validation.constraints.NotNull;

public record SolveLessonPuzzleRequest(
        @NotNull
        Long puzzleId
) { }
