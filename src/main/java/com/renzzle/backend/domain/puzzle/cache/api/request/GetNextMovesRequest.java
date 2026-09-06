package com.renzzle.backend.domain.puzzle.cache.api.request;

import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Parameters for prefetching the cached replies one ply ahead.
 * {@code userTurnBoardState} is a position where it is the <b>user's</b> turn,
 * unlike the AI-turn position that {@code /ai-response} takes.
 */
public record GetNextMovesRequest(
        @NotNull(message = "Puzzle type is required")
        PuzzleType puzzleType,

        @NotNull(message = "Puzzle ID is required")
        Long puzzleId,

        @NotBlank(message = "Board status is required")
        String userTurnBoardState
) { }
