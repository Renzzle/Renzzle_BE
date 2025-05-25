package com.renzzle.backend.domain.puzzle.content.api.request;

import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.validation.ValidEnum;

public record GetRecommendRequest(
        @ValidEnum(enumClass = LangCode.LangCodeName.class, message = "잘못된 lang 형식입니다")
        String langCode
) { }
