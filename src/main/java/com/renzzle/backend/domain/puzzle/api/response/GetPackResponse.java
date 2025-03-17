package com.renzzle.backend.domain.puzzle.api.response;

public record GetPackResponse(
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
