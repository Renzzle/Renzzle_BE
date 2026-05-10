package com.renzzle.backend.domain.puzzle.cache.api.response;

import lombok.Builder;

@Builder
public record CommunityPuzzleCachePickerResponse(
        long id,
        String boardStatus,
        int depth,
        String winColor,
        String description,
        String authorNickname
) {
}
