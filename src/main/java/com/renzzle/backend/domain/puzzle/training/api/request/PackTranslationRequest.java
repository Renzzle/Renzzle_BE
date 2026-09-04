package com.renzzle.backend.domain.puzzle.training.api.request;

import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.NotBlank;

public record PackTranslationRequest(
        @ValidEnum(enumClass = LangCode.LangCodeName.class, message = "Invalid lang format")
        String langCode,

        @NotBlank(message = "title is required")
        String title,

        @NotBlank(message = "author is required")
        String author,

        String description
) { }
