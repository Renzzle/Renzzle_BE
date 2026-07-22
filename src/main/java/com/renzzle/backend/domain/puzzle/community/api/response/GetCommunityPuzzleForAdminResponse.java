package com.renzzle.backend.domain.puzzle.community.api.response;

import lombok.Builder;

@Builder
public record GetCommunityPuzzleForAdminResponse(
        long id,
        String boardStatus,
        String answer,
        String authorName,
        String description,
        int depth,
        String winColor,
        int solvedCount,
        int views,
        int likeCount,
        String createdAt,
        boolean isVerified
) {
}
