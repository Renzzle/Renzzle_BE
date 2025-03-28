package com.renzzle.backend.domain.puzzle.rank.api.response;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import lombok.Builder;

@Builder
public record RankStartResponse(
        String boardStatus,
        String winColor
) {
}
