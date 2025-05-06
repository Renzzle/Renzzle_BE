package com.renzzle.backend.domain.puzzle.content.api.response;

import lombok.Builder;

@Builder
public record getRecommendPackResponse(
        Long id,
        String title,
        String author,
        String description,
        int price,
        int totalPuzzleCount,
        int solvedPuzzleCount,
        boolean locked
) {
}
