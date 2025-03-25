package com.renzzle.backend.domain.puzzle.api.request;

import com.renzzle.backend.global.common.constant.LanguageCode;
import jakarta.validation.constraints.NotBlank;

public record PackTranslationRequest(
        @NotBlank(message = "langCode는 필수입니다")
        LanguageCode langCode,

        @NotBlank(message = "title은 필수입니다")
        String title,

        @NotBlank(message = "author는 필수입니다")
        String author,

        String description
) { }
