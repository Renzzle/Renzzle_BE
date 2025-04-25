package com.renzzle.backend.domain.puzzle.content.api.response;

import lombok.Builder;

@Builder
public record getTrendPuzzleResponse(
    long id,
    String boardStatus,
    long authorId,
    String authorName,
    int depth,
    String winColor,
    int likeCount,
    boolean isVerified
) {
}
