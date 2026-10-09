package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.domain.auth.api.request.ReissueTokenRequest;
import com.renzzle.backend.domain.auth.api.response.LoginResponse;
import com.renzzle.backend.domain.auth.dao.RefreshSessionRepository;
import com.renzzle.backend.domain.auth.service.JwtProvider.TokenClaims;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private Clock clock;
    @Mock
    private RefreshSessionRepository refreshSessionRepository;
    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    private final String FIXED_TIME = "2025-03-20T10:00:00Z";

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse(FIXED_TIME));
    }

    @Test
    void createAndVerifyAuthVerityToken_ShouldVerify() {
        String email = "test@example.com";
        String validToken = "valid-token";
        when(jwtProvider.createAuthVerityToken(email)).thenReturn(validToken);

        String token = authService.createAuthVerityToken(email);

        assertEquals(validToken, token);

        when(jwtProvider.getEmail(validToken)).thenReturn(email);

        boolean result = authService.verifyAuthVerityToken(validToken, email);

        assertTrue(result);
    }

    @Test
    void createAuthTokens_WhenCalled_ThenOpensSessionHoldingTheRefreshToken() {
        // given
        when(jwtProvider.createAccessToken(eq(1L), anyString())).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(eq(1L), anyString())).thenReturn("refresh-token");

        // when
        LoginResponse response = authService.createAuthTokens(1L);

        // then
        assertEquals("Bearer", response.grantType());
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(Instant.parse(FIXED_TIME).plus(Duration.ofMinutes(60)), response.accessTokenExpiredAt());
        assertEquals(Instant.parse(FIXED_TIME).plus(Duration.ofDays(14)), response.refreshTokenExpiredAt());

        ArgumentCaptor<String> sessionId = ArgumentCaptor.forClass(String.class);
        verify(jwtProvider).createAccessToken(eq(1L), sessionId.capture());
        verify(jwtProvider).createRefreshToken(1L, sessionId.getValue());
        verify(refreshSessionRepository).save(1L, sessionId.getValue(), "refresh-token");
    }

    @Test
    void createAuthTokens_WhenLoggedInTwice_ThenEachLoginGetsItsOwnSession() {
        // given
        when(jwtProvider.createAccessToken(eq(1L), anyString())).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(eq(1L), anyString())).thenReturn("refresh-token");

        // when
        authService.createAuthTokens(1L);
        authService.createAuthTokens(1L);

        // then
        ArgumentCaptor<String> sessionIds = ArgumentCaptor.forClass(String.class);
        verify(refreshSessionRepository, times(2)).save(eq(1L), sessionIds.capture(), eq("refresh-token"));
        List<String> ids = sessionIds.getAllValues();
        assertNotEquals(ids.get(0), ids.get(1));
    }

    @Test
    void reissueToken_WhenTokenIsTheSessionsLatest_ThenRotatesWithinTheSameSession() {
        // given
        when(jwtProvider.parseRefreshToken("old-refresh")).thenReturn(new TokenClaims(1L, "session-1"));
        when(refreshSessionRepository.findToken("session-1")).thenReturn(Optional.of("old-refresh"));
        when(jwtProvider.createAccessToken(1L, "session-1")).thenReturn("new-access");
        when(jwtProvider.createRefreshToken(1L, "session-1")).thenReturn("new-refresh");

        // when
        LoginResponse response = authService.reissueToken(new ReissueTokenRequest("old-refresh"));

        // then
        assertEquals("new-access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
        verify(refreshSessionRepository).save(1L, "session-1", "new-refresh");
    }

    @Test
    void reissueToken_WhenTokenWasAlreadyReissued_ThenRejectsIt() {
        // given: the session has moved on to a newer refresh token
        when(jwtProvider.parseRefreshToken("stale-refresh")).thenReturn(new TokenClaims(1L, "session-1"));
        when(refreshSessionRepository.findToken("session-1")).thenReturn(Optional.of("newer-refresh"));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.reissueToken(new ReissueTokenRequest("stale-refresh")));

        // then
        assertEquals(ErrorCode.EXPIRED_JWT_TOKEN, exception.getErrorCode());
        verify(refreshSessionRepository, never()).save(anyLong(), anyString(), anyString());
    }

    @Test
    void reissueToken_WhenSessionWasRevoked_ThenRejectsIt() {
        // given
        when(jwtProvider.parseRefreshToken("refresh")).thenReturn(new TokenClaims(1L, "session-1"));
        when(refreshSessionRepository.findToken("session-1")).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.reissueToken(new ReissueTokenRequest("refresh")));

        // then
        assertEquals(ErrorCode.EXPIRED_JWT_TOKEN, exception.getErrorCode());
    }

    @Test
    void logout_WhenSessionKnown_ThenEndsOnlyThatSession() {
        // given
        UserEntity user = UserEntity.builder().id(1L).build();

        // when
        Long userId = authService.logout(user, "session-1");

        // then
        assertEquals(1L, userId);
        verify(refreshSessionRepository).delete(1L, "session-1");
        verify(refreshSessionRepository, never()).deleteAll(anyLong());
    }

}
