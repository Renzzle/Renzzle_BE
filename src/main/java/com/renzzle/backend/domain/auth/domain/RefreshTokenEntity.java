package com.renzzle.backend.domain.auth.domain;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import static com.renzzle.backend.domain.auth.service.JwtProvider.REFRESH_TOKEN_VALID_MINUTE;

@Builder
@RedisHash(value = "refreshToken", timeToLive = 60 * REFRESH_TOKEN_VALID_MINUTE)
public record RefreshTokenEntity(
        @Id
        Long id,
        String token
) { }
