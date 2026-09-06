package com.renzzle.backend.domain.auth.api.request;

import jakarta.validation.constraints.NotEmpty;

public record ReissueTokenRequest(
        @NotEmpty(message = "Refresh token is required")
        String refreshToken
) { }
