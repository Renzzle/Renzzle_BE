package com.renzzle.backend.domain.puzzle.community.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCommunityPuzzleVerificationRequest(
        @NotNull(message = "Verification flag is required")
        Boolean isVerified,

        // Optional corrected answer, only taken while verifying
        @Size(max = 1023, message = "Answer must be at most 1023 characters")
        String answer
) {
}
