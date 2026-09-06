package com.renzzle.backend.domain.puzzle.community.api.request;

import jakarta.validation.constraints.NotNull;

public record CommunityPuzzleResultUpdateRequest(
        @NotNull(message = "Puzzle ID is required")
        Long puzzleId
) { }
