package com.renzzle.backend.domain.notice.api.request;

import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnouncementContentRequest(
        @ValidEnum(enumClass = LangCode.LangCodeName.class, message = "Invalid lang format")
        String langCode,

        @NotBlank(message = "Title is required")
        @Size(max = 127, message = "Title must be 127 characters or fewer")
        String title,

        @NotBlank(message = "Context is required")
        @Size(max = 1023, message = "Context must be 1023 characters or fewer")
        String context
) { }
