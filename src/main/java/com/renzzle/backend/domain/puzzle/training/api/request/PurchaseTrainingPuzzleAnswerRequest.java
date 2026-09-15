package com.renzzle.backend.domain.puzzle.training.api.request;

import jakarta.validation.constraints.NotNull;

public record PurchaseTrainingPuzzleAnswerRequest(

        @NotNull(message = "Puzzle ID is required")
        Long puzzleId
) {
}
