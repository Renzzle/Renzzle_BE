package com.renzzle.backend.domain.user.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GetUserPuzzleRequest(
        Long id,

        @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size must be at most 100")
        Integer size
) {
    public int sizeOrDefault() {
        return size != null ? size : 10;
    }
}
