package com.renzzle.backend.domain.user.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GetUserPuzzleRequest(
        @Min(value = 0, message = "page must be at least 0")
        Integer page,

        @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size must be at most 100")
        Integer size
) { }