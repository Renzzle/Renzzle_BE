package com.renzzle.backend.domain.auth.api.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record LoginResponse(
        String grantType,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiredAt,
        Instant refreshTokenExpiredAt
) { }
