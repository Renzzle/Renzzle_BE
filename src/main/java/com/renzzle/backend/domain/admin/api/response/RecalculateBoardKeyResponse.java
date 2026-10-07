package com.renzzle.backend.domain.admin.api.response;

import com.renzzle.backend.domain.puzzle.shared.dto.BoardKeyRecalculationResult;

public record RecalculateBoardKeyResponse(
        BoardKeyRecalculationResult training,
        BoardKeyRecalculationResult community
) {
}
