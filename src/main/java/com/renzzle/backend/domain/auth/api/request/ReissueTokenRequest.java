package com.renzzle.backend.domain.auth.api.request;

import jakarta.validation.constraints.NotBlank;

public record ReissueTokenRequest(
        @NotBlank(message = "토큰 정보가 없습니다")
        String refreshToken
) { }
