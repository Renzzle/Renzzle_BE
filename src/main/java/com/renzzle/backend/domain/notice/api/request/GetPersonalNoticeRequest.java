package com.renzzle.backend.domain.notice.api.request;

import com.renzzle.backend.domain.notice.util.AppVersionUtil;
import com.renzzle.backend.global.common.domain.AppPlatform;
import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GetPersonalNoticeRequest(
        @ValidEnum(enumClass = LangCode.LangCodeName.class, message = "Invalid lang format")
        String langCode,

        @ValidEnum(enumClass = AppPlatform.class, message = "Invalid platform format")
        String platform,

        @NotBlank(message = "Version is required")
        @Pattern(regexp = AppVersionUtil.VERSION_REGEX, message = "Invalid version format")
        String version
) { }
