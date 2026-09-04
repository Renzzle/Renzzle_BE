package com.renzzle.backend.domain.puzzle.training.api.request;

import jakarta.validation.constraints.NotNull;

public record PurchaseTrainingPuzzleAnswerRequest(

        @NotNull(message = "Invalid puzzle ID")
        Long puzzleId
) {
}
