package com.renzzle.backend.domain.puzzle.rank.api.request;

import jakarta.validation.constraints.NotNull;

public record RankResultRequest(
        @NotNull(message = "풀이 여부가 존재해야 합니다.")
        boolean isSolved
) { }
