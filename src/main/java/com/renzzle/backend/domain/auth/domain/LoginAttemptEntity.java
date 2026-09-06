package com.renzzle.backend.domain.auth.domain;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

// Kept for a day after the last failure so the lock duration keeps escalating across lock cycles
@Builder
@RedisHash(value = "loginAttempt", timeToLive = 60 * 60 * 24)
public record LoginAttemptEntity(
        @Id
        String email,
        int failCount,
        String lockedUntil
) { }
