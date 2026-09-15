package com.renzzle.backend.domain.notice.api.request;

import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.validation.ValidEnum;

public record GetPublicNoticeRequest(
        @ValidEnum(enumClass = LangCode.LangCodeName.class, message = "Invalid lang format")
        String langCode
) { }
