package com.renzzle.backend.domain.puzzle.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GetLessonPuzzleRequest(
        @Min(value = 0, message = "page는 최소 0이어야 합니다")
        Integer page,

        @Min(value = 1, message = "size는 최소 1이어야 합니다")
        @Max(value = 100, message = "size는 최대 100이어야 합니다")
        Integer size
) { }
