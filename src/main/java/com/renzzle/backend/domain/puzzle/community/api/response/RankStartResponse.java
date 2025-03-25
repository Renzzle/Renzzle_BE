package com.renzzle.backend.domain.puzzle.community.api.response;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import lombok.Builder;

@Builder
public record RankStartResponse(
        Long sessionId,
        String boardStatus,
        WinColor winColor
) {
}
