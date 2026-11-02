package com.renzzle.backend.domain.puzzle.community.api.response;

import lombok.Builder;

@Builder
public record GetSharedPuzzleResponse(
        String boardStatus,
        String authorName,
        String description,
        int depth,
        String winColor,
        boolean isVerified
) { }
