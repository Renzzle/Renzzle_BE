package com.renzzle.backend.domain.user.api.response;

import lombok.Builder;

import java.util.List;

@Builder
public record LikeResponse(
        Long id,
        String title,
        String boardStatus,
        Long authorId,
        String authorName,
        int solvedCount,
        double correctRate,
        int depth,
        String difficulty,
        String winColor,
        int likeCount,
        List<String> tag
) {
}
