package com.renzzle.backend.domain.puzzle.community.api.response;

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
        String difficulty,
        String winColor,
        int likeCount,
        List<String> tag
) { }
