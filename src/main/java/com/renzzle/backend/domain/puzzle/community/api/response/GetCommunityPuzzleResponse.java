package com.renzzle.backend.domain.puzzle.community.api.response;

import lombok.Builder;
import java.util.List;

@Builder
public record GetCommunityPuzzleResponse(
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
