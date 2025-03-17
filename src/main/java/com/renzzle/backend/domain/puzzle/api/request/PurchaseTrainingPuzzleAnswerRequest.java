package com.renzzle.backend.domain.puzzle.api.request;

import jakarta.validation.constraints.NotNull;

public record PurchaseTrainingPuzzleAnswerRequest(

        @NotNull(message = "퍼즐 아이디가 올바르지 않습니다")
        Long puzzleId
) {
}
