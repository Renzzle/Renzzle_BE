package com.renzzle.backend.domain.puzzle.community.api.response;

import lombok.Builder;

@Builder
public record AddPuzzleResponse(
        long puzzleId
) { }
