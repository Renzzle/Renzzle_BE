package com.renzzle.backend.domain.puzzle.api.response;

import com.renzzle.backend.domain.puzzle.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.domain.WinColor;
import lombok.Builder;
import java.util.List;

@Builder
public record GetCommunityPuzzleResponse(
        long id,
        String title,
        String boardStatus,
        long authorId,
        String authorName,
        int solvedCount,
        double correctRate,
        int depth,
        Difficulty difficulty,
        WinColor winColor,
        int likeCount,
        List<String> tag
) { }
