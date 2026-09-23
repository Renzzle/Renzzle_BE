package com.renzzle.backend.domain.notice.api.request;

import com.renzzle.backend.global.common.domain.AppPlatform;
import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.NotBlank;

public record GetPersonalNoticeRequest(
        @ValidEnum(enumClass = LangCode.LangCodeName.class, message = "Invalid lang format")
        String langCode,

        @ValidEnum(enumClass = AppPlatform.class, message = "Invalid platform format")
        String platform,

        @NotBlank(message = "Version is required")
        String version
) { }
