package com.renzzle.backend.domain.puzzle.rank.support;

import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.Status;

import java.util.UUID;

import static com.renzzle.backend.global.common.constant.TimeConstant.CONST_FUTURE_INSTANT;
import static com.renzzle.backend.support.TestTime.FIXED_INSTANT;

public class TestUserFactory {

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
                .lastAccessedAt(FIXED_INSTANT)
                .deletedAt(CONST_FUTURE_INSTANT)
                .build();
    }
}
