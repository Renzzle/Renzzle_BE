package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.domain.auth.dao.LoginAttemptRedisRepository;
import com.renzzle.backend.domain.auth.domain.LoginAttemptEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static com.renzzle.backend.domain.auth.service.LoginAttemptService.LOGIN_FAIL_LIMIT;
import static com.renzzle.backend.domain.auth.service.LoginAttemptService.LOGIN_LOCK_BASE_SECOND;
import static com.renzzle.backend.domain.auth.service.LoginAttemptService.LOGIN_LOCK_MAX_SECOND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private Clock clock;
    @Mock
    private LoginAttemptRedisRepository loginAttemptRepository;
    @Captor
    private ArgumentCaptor<LoginAttemptEntity> attemptCaptor;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    private static final String FIXED_TIME = "2025-03-20T10:00:00Z";
    private static final String EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse(FIXED_TIME));
    }

    private LoginAttemptEntity attempt(int failCount, String lockedUntil) {
        return LoginAttemptEntity.builder()
                .email(EMAIL)
                .failCount(failCount)
                .lockedUntil(lockedUntil)
                .build();
    }

    private String savedLockedUntil() {
        verify(loginAttemptRepository).save(attemptCaptor.capture());
        return attemptCaptor.getValue().lockedUntil();
    }

    @Test
    void checkNotLocked_ShouldPass_WhenNoFailureRecorded() {
        // given
        when(loginAttemptRepository.findById(EMAIL)).thenReturn(Optional.empty());

        // when & then
        assertDoesNotThrow(() -> loginAttemptService.checkNotLocked(EMAIL));
    }

    @Test
    void checkNotLocked_ShouldPass_WhenBelowLimit() {
        // given
        when(loginAttemptRepository.findById(EMAIL)).thenReturn(Optional.of(attempt(LOGIN_FAIL_LIMIT - 1, null)));

        // when & then
        assertDoesNotThrow(() -> loginAttemptService.checkNotLocked(EMAIL));
    }

    @Test
    void checkNotLocked_ShouldPass_WhenLockAlreadyExpired() {
        // given
        String expiredAt = Instant.parse(FIXED_TIME).minusSeconds(1).toString();
        when(loginAttemptRepository.findById(EMAIL)).thenReturn(Optional.of(attempt(LOGIN_FAIL_LIMIT, expiredAt)));

        // when & then
        assertDoesNotThrow(() -> loginAttemptService.checkNotLocked(EMAIL));
    }

    @Test
    void checkNotLocked_ShouldThrowException_WhenStillLocked() {
        // given
        String lockedUntil = Instant.parse(FIXED_TIME).plusSeconds(1).toString();
        when(loginAttemptRepository.findById(EMAIL)).thenReturn(Optional.of(attempt(LOGIN_FAIL_LIMIT, lockedUntil)));

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> loginAttemptService.checkNotLocked(EMAIL));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXCEED_LOGIN_ATTEMPT);
    }

    @Test
    void recordFailure_ShouldNotLock_BelowLimit() {
        // given
        when(loginAttemptRepository.findById(EMAIL)).thenReturn(Optional.of(attempt(LOGIN_FAIL_LIMIT - 2, null)));

        // when
        loginAttemptService.recordFailure(EMAIL);

        // then
        verify(loginAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().failCount()).isEqualTo(LOGIN_FAIL_LIMIT - 1);
        assertThat(attemptCaptor.getValue().lockedUntil()).isNull();
    }

    @Test
    void recordFailure_ShouldLockForBaseDuration_WhenLimitIsReached() {
        // given
        when(loginAttemptRepository.findById(EMAIL)).thenReturn(Optional.of(attempt(LOGIN_FAIL_LIMIT - 1, null)));

        // when
        loginAttemptService.recordFailure(EMAIL);

        // then
        assertThat(savedLockedUntil())
                .isEqualTo(Instant.parse(FIXED_TIME).plusSeconds(LOGIN_LOCK_BASE_SECOND).toString());
    }

    @Test
    void recordFailure_ShouldDoubleLockDuration_WhenFailingAgainAfterLock() {
        // given
        when(loginAttemptRepository.findById(EMAIL)).thenReturn(Optional.of(attempt(LOGIN_FAIL_LIMIT, null)));

        // when
        loginAttemptService.recordFailure(EMAIL);

        // then
        assertThat(savedLockedUntil())
                .isEqualTo(Instant.parse(FIXED_TIME).plusSeconds(LOGIN_LOCK_BASE_SECOND * 2L).toString());
    }

    @Test
    void recordFailure_ShouldCapLockDuration() {
        // given
        when(loginAttemptRepository.findById(EMAIL)).thenReturn(Optional.of(attempt(LOGIN_FAIL_LIMIT + 20, null)));

        // when
        loginAttemptService.recordFailure(EMAIL);

        // then
        assertThat(savedLockedUntil())
                .isEqualTo(Instant.parse(FIXED_TIME).plusSeconds(LOGIN_LOCK_MAX_SECOND).toString());
    }

    @Test
    void recordFailure_ShouldStartFromOne_WhenNoRecordExists() {
        // given
        when(loginAttemptRepository.findById(EMAIL)).thenReturn(Optional.empty());

        // when
        loginAttemptService.recordFailure(EMAIL);

        // then
        verify(loginAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().failCount()).isEqualTo(1);
        assertThat(attemptCaptor.getValue().lockedUntil()).isNull();
    }

    @Test
    void reset_ShouldDeleteRecord() {
        // when
        loginAttemptService.reset(EMAIL);

        // then
        verify(loginAttemptRepository).deleteById(EMAIL);
    }

}
