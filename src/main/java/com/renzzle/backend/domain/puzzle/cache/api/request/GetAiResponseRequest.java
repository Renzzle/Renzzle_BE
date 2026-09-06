package com.renzzle.backend.domain.puzzle.cache.api.request;

import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GetAiResponseRequest(
        @NotNull(message = "Puzzle type is required")
        PuzzleType puzzleType,

        @NotNull(message = "Puzzle ID is required")
        Long puzzleId,

        @NotBlank(message = "Board status is required")
        String currentBoardState
) { }
