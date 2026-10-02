package com.renzzle.backend.domain.admin.api.response;

public record RecalculatePuzzleRatingResponse(
        int trainingPuzzleCount,
        int communityPuzzleCount
) {
}
