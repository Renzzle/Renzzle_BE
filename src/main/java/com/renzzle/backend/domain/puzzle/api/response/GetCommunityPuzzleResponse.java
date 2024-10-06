package com.renzzle.backend.domain.puzzle.api.response;

import lombok.Builder;

import java.util.ArrayList;

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
        ArrayList<String> tag
) { }
