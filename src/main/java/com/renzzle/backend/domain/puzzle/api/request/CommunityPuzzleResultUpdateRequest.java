package com.renzzle.backend.domain.puzzle.api.request;

import jakarta.validation.constraints.NotNull;

public record CommunityPuzzleResultUpdateRequest(
        @NotNull(message = "퍼즐 아이디 정보가 없습니다")
        Long puzzleId
) { }
