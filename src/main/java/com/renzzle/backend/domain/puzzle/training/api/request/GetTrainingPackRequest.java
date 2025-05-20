package com.renzzle.backend.domain.puzzle.training.api.request;

import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.NotBlank;

public record GetTrainingPackRequest(
        @NotBlank(message = "Difficulty is required")
        String difficulty,

        @ValidEnum(enumClass = LangCode.LangCodeName.class, message = "잘못된 lang 형식입니다")
        String lang
) { }
