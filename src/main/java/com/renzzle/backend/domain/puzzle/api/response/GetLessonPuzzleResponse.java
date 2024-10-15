package com.renzzle.backend.domain.puzzle.api.response;

import lombok.Builder;

@Builder
public record GetLessonPuzzleResponse(
        long id,
        String title,
        String boardStatus,
        int depth,
        String difficulty,
        String winColor,
        String description,
        boolean isLocked
) { }
