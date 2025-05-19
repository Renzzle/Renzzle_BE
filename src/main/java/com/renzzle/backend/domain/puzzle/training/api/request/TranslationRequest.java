package com.renzzle.backend.domain.puzzle.training.api.request;

import com.renzzle.backend.global.common.constant.LanguageCode;
import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TranslationRequest(

        @NotNull(message = "packId는 필수입니다")
        Long packId,

        @ValidEnum(enumClass = LangCode.LangCodeName.class, message = "잘못된 lang 형식입니다")
        String langCode,

        @NotBlank(message = "title은 필수입니다")
        String title,

        @NotBlank(message = "author는 필수입니다")
        String author,

        String description
) { }
