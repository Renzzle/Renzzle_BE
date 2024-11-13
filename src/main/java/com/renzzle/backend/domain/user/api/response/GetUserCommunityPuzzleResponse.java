package com.renzzle.backend.domain.user.api.response;

import lombok.Builder;

import java.util.List;

@Builder
public record GetUserCommunityPuzzleResponse(

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

) {
}
