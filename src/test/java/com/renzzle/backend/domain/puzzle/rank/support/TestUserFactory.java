package com.renzzle.backend.domain.puzzle.rank.support;

import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.Status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.renzzle.backend.global.common.constant.TimeConstant.CONST_FUTURE_INSTANT;

public class TestUserFactory {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    public static UserEntity createTestUser(String nickname, double rating) {

        return UserEntity.builder()
                .nickname(nickname)
                .email(nickname + "@test.com")
                .password("test123")
                .rating(rating)
                .mmr(rating)
                .currency(1000)
                .deviceId(UUID.randomUUID().toString())
                .status(Status.getDefaultStatus())
                .lastAccessedAt(Instant.now(FIXED_CLOCK))
                .deletedAt(CONST_FUTURE_INSTANT)
                .build();
    }
}
