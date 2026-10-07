package com.renzzle.backend.domain.admin.api.response;

import com.renzzle.backend.domain.puzzle.shared.dto.AnswerKeyRecalculationResult;

public record RecalculateAnswerKeyResponse(
        AnswerKeyRecalculationResult training,
        AnswerKeyRecalculationResult community
) {
}
