package com.renzzle.backend.domain.puzzle.api.request;

import com.renzzle.backend.global.common.constant.LanguageCode;
import jakarta.validation.constraints.NotBlank;

public record GetTrainingPackRequest(
        @NotBlank(message = "Difficulty is required")
        String difficulty,

        LanguageCode lang
) {
    public GetTrainingPackRequest {
        if (lang == null) {
            lang = LanguageCode.en;
        }
    }
}
