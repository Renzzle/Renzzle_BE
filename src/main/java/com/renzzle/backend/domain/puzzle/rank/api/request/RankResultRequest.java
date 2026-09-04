package com.renzzle.backend.domain.puzzle.rank.api.request;

import jakarta.validation.constraints.NotNull;

public record RankResultRequest(
        @NotNull(message = "Solved flag is required")
        boolean isSolved
) { }
