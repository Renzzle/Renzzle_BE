package com.renzzle.backend.domain.puzzle.cache.api.request;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Parameters for querying the community puzzle list for the puzzle cache entry UI.
 */
public record GetCommunityPuzzlesForCacheRequest(
        @Size(max = 31, message = "Nickname must be at most 31 characters")
        String authorNickname,

        @ValidEnum(enumClass = WinColor.WinColorName.class, nullable = true, message = "Invalid stone format")
        String stone,

        @Min(value = 1, message = "depthMin must be at least 1")
        @Max(value = 225, message = "depthMin must be at most 225")
        Integer depthMin,

        @Min(value = 1, message = "depthMax must be at least 1")
        @Max(value = 225, message = "depthMax must be at most 225")
        Integer depthMax,

        Long id,

        @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size must be at most 100")
        Integer size
) {
}
