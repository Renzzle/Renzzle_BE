package com.renzzle.backend.domain.puzzle.community.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// board is a user-to-move position, as in /api/puzzle/cache/next-moves
public record GetSharedNextMovesRequest(
        @NotBlank(message = "Board status is required")
        @Size(max = 1023, message = "Board status is too long")
        String board
) { }
