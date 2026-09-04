package com.renzzle.backend.domain.puzzle.community.api.request;

import jakarta.validation.constraints.NotNull;

public record UpdateCommunityPuzzleVerificationRequest(
        @NotNull(message = "Verification flag is required")
        Boolean isVerified
) {
}
