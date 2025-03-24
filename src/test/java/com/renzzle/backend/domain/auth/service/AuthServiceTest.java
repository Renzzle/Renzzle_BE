package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.domain.auth.api.request.ReissueTokenRequest;
import com.renzzle.backend.domain.auth.api.response.LoginResponse;
import com.renzzle.backend.domain.auth.dao.RefreshTokenRedisRepository;
import com.renzzle.backend.domain.auth.domain.RefreshTokenEntity;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private Clock clock;
    @Mock
    private RefreshTokenRedisRepository refreshTokenRepository;
    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    private final String FIXED_TIME = "2025-03-20T10:00:00Z";

    @BeforeEach
    public void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse(FIXED_TIME));
    }

    @Test
    public void createAndVerifyAuthVerityToken_ShouldVerify() {
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
    public void createAuthTokens_ShouldReturnLoginResponse() {
        // given
        String token = "token";
        when(jwtProvider.createAccessToken(any(Long.class))).thenReturn(token);
        when(jwtProvider.createRefreshToken(any(Long.class))).thenReturn(token);

        // when
        LoginResponse response = authService.createAuthTokens(1L);

        // then
        assertNotNull(response);
        assertEquals("Bearer", response.grantType());
        assertEquals(token, response.accessToken());
        assertEquals(token, response.refreshToken());

        Instant expectedAccessTokenExpiry = Instant.parse(FIXED_TIME).plus(Duration.ofMinutes(60));
        Instant expectedRefreshTokenExpiry = Instant.parse(FIXED_TIME).plus(Duration.ofDays(14));
        assertEquals(expectedAccessTokenExpiry, response.accessTokenExpiredAt());
        assertEquals(expectedRefreshTokenExpiry, response.refreshTokenExpiredAt());

        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
    }

    @Test
    public void reissueToken_ShouldReturnNewTokens_WhenRefreshTokenIsValid() {
        // given
        Long userId = 1L;
        String refreshToken = "valid-refresh-token";
        ReissueTokenRequest request = new ReissueTokenRequest(refreshToken);

        UserEntity user = mock(UserEntity.class);
        when(user.getId()).thenReturn(userId);
        when(jwtProvider.getUserId(refreshToken)).thenReturn(userId);
        when(refreshTokenRepository.findById(userId)).thenReturn(Optional.of(mock(RefreshTokenEntity.class)));
        when(jwtProvider.createAccessToken(userId)).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(userId)).thenReturn("refresh-token");

        // when
        LoginResponse response = authService.reissueToken(user, request);

        // then
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
    }

    @Test
    public void reissueToken_ShouldThrowException_WhenRefreshTokenInvalid() {
        // given
        Long userId = 1L;
        String invalidRefreshToken = "invalid-refresh-token";
        ReissueTokenRequest request = new ReissueTokenRequest(invalidRefreshToken);

        UserEntity user = mock(UserEntity.class);
        when(jwtProvider.getUserId(invalidRefreshToken)).thenReturn(userId);
        when(refreshTokenRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () ->
                authService.reissueToken(user, request));

        assertEquals(ErrorCode.EXPIRED_JWT_TOKEN, exception.getErrorCode());
    }

}
