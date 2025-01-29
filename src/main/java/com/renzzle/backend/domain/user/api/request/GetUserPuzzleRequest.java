package com.renzzle.backend.domain.user.api.request;

import com.renzzle.backend.global.common.constant.SortOption;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GetUserPuzzleRequest(
        @Min(value = 0, message = "page는 최소 0이어야 합니다")
        Integer page,

        @Min(value = 1, message = "size는 최소 1이어야 합니다")
        @Max(value = 100, message = "size는 최대 100이어야 합니다")
        Integer size
) { }