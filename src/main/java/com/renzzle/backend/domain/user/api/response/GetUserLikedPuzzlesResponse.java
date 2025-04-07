package com.renzzle.backend.domain.user.api.response;

import lombok.Builder;

@Builder
public record GetUserLikedPuzzlesResponse(
        long id,
        String boardStatus,
        long authorId,
        String authorName,
        int depth,
        String winColor,
        int likeCount,
        String createdAt,
        boolean isSolved,
        boolean isVerified
) { }
