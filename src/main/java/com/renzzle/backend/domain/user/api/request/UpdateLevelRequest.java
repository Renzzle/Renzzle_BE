package com.renzzle.backend.domain.user.api.request;

import jakarta.validation.constraints.NotNull;

public record UpdateLevelRequest(
        @NotNull(message = "레벨은 반드시 지정되어야 합니다.")
        String level
) {
}
