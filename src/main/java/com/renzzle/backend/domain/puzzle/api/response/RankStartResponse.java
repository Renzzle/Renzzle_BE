package com.renzzle.backend.domain.puzzle.api.response;

import com.renzzle.backend.domain.puzzle.domain.WinColor;
import lombok.Builder;

@Builder
public record RankStartResponse(
        Long sessionId,
        String boardStatus,
        WinColor winColor
) {
}
