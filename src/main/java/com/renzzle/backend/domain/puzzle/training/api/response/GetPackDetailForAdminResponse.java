package com.renzzle.backend.domain.puzzle.training.api.response;

import java.util.List;

public record GetPackDetailForAdminResponse(
        Long id,
        List<TranslationInfo> info,
        Integer price,
        String difficulty,
        int totalPuzzleCount
) {
    public record TranslationInfo(
            String langCode,
            String title,
            String author,
            String description
    ) {}
}
