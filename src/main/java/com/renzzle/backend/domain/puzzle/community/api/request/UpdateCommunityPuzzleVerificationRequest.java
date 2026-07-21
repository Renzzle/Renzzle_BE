package com.renzzle.backend.domain.puzzle.community.api.request;

import jakarta.validation.constraints.NotNull;

public record UpdateCommunityPuzzleVerificationRequest(
        @NotNull(message = "검증 여부 정보가 없습니다")
        Boolean isVerified
) {
}
