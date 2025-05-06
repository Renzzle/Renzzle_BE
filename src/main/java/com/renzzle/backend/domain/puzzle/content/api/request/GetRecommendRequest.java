package com.renzzle.backend.domain.puzzle.content.api.request;

import com.renzzle.backend.global.common.constant.LanguageCode;
import jakarta.validation.constraints.NotBlank;

public record GetRecommendRequest(
        @NotBlank(message = "Difficulty is required")
        String difficulty,

        LanguageCode lang
) {
    public GetRecommendRequest {
        if (lang == null) {
            lang = LanguageCode.en;
        }
    }
}
