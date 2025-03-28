package com.renzzle.backend.domain.puzzle.rank.api.response;

import lombok.Builder;

@Builder
public record RankResultResponse(
        String boardStatus,
        String winColor
) { }
