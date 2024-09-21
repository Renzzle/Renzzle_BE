package com.renzzle.backend.domain.auth.api.request;

public record ReissueTokenRequest(
        String refreshToken
) { }
