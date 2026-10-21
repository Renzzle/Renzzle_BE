package com.renzzle.backend.domain.puzzle.training.api.response;

public record GetPackForAdminResponse(
        Long id,
        String title,
        String author,
        String description,
        int price,
        int totalPuzzleCount,
        boolean isPublic,
        String creatorEmail
) {
}
