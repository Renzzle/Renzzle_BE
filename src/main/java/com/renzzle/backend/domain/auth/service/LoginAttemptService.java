package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.domain.auth.dao.LoginAttemptRedisRepository;
import com.renzzle.backend.domain.auth.domain.LoginAttemptEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    public static final int LOGIN_FAIL_LIMIT = 10;
    public static final int LOGIN_LOCK_BASE_SECOND = 5 * 60; // 5 minute
    public static final int LOGIN_LOCK_MAX_SECOND = 60 * 60; // 1 hour

    private static final int MAX_LOCK_STEP = 16; // guards the shift below from overflowing

    private final Clock clock;
    private final LoginAttemptRedisRepository loginAttemptRepository;

    public void checkNotLocked(String email) {
        loginAttemptRepository.findById(email).ifPresent(attempt -> {
            if (attempt.lockedUntil() == null) return;
            if (clock.instant().isBefore(Instant.parse(attempt.lockedUntil()))) {
                throw new CustomException(ErrorCode.EXCEED_LOGIN_ATTEMPT);
            }
        });
    }

    public void recordFailure(String email) {
        int failCount = loginAttemptRepository.findById(email)
                .map(LoginAttemptEntity::failCount)
                .orElse(0) + 1;

        String lockedUntil = null;
        if (failCount >= LOGIN_FAIL_LIMIT) {
            lockedUntil = clock.instant().plusSeconds(lockSecondOf(failCount)).toString();
        }

        loginAttemptRepository.save(LoginAttemptEntity.builder()
                .email(email)
                .failCount(failCount)
                .lockedUntil(lockedUntil)
                .build());
    }

    public void reset(String email) {
        loginAttemptRepository.deleteById(email);
    }

    // The failure that hits the limit locks for the base duration and every further one doubles it
    private long lockSecondOf(int failCount) {
        int step = Math.min(failCount - LOGIN_FAIL_LIMIT, MAX_LOCK_STEP);
        long lockSecond = (long) LOGIN_LOCK_BASE_SECOND << step;
        return Math.min(lockSecond, LOGIN_LOCK_MAX_SECOND);
    }

}
