package com.renzzle.backend.domain.auth.domain;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Builder
@RedisHash(value = "email", timeToLive = 60 * 5)
public record AuthEmailEntity(
        @Id
        String email,
        String code
) { }
