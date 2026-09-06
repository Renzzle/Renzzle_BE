package com.renzzle.backend.domain.auth.domain;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Builder
@RedisHash(value = "email", timeToLive = 60 * 60 * 24)
public record AuthEmailEntity(
        @Id
        String email,
        String code,
        int count,
        int attemptCount,
        boolean verified,
        String issuedAt
) {

    public AuthEmailEntity increaseAttemptCount() {
        return new AuthEmailEntity(email, code, count, attemptCount + 1, verified, issuedAt);
    }

    public AuthEmailEntity verify() {
        return new AuthEmailEntity(email, code, count, attemptCount, true, issuedAt);
    }

}
