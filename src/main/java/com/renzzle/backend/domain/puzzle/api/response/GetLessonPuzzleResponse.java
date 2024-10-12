package com.renzzle.backend.domain.puzzle.api.response;

import com.renzzle.backend.domain.puzzle.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.domain.WinColor;
import lombok.Builder;

@Builder
public record GetLessonPuzzleResponse(
        long id,
        String title,
        String boardStatus,
        int depth,
        Difficulty difficulty,
        WinColor winColor,
        String description,
        boolean isLocked
) { }
