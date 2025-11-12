package com.renzzle.backend.domain.puzzle.community.api.response;

import lombok.Builder;

@Builder
public record GetCommunityPuzzlesResponse(
        long id,
        String boardStatus,
        long authorId,
        String authorName,
        String description,
        int depth,
        String winColor,
        int solvedCount,
        int views,
        int likeCount,
        String createdAt,
        boolean isSolved,
        boolean isVerified
) { }
