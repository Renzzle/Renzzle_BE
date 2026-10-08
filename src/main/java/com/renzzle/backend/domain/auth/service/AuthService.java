package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.domain.auth.api.request.ReissueTokenRequest;
import com.renzzle.backend.domain.auth.api.response.LoginResponse;
import com.renzzle.backend.domain.auth.dao.RefreshSessionRepository;
import com.renzzle.backend.domain.auth.domain.GrantType;
import com.renzzle.backend.domain.auth.service.JwtProvider.TokenClaims;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import static com.renzzle.backend.domain.auth.service.JwtProvider.ACCESS_TOKEN_VALID_MINUTE;
import static com.renzzle.backend.domain.auth.service.JwtProvider.REFRESH_TOKEN_VALID_MINUTE;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final Clock clock;
    private final RefreshSessionRepository refreshSessionRepository;
    private final JwtProvider jwtProvider;

    public String createAuthVerityToken(String email) {
        return jwtProvider.createAuthVerityToken(email);
    }

    public boolean verifyAuthVerityToken(String token, String email) {
        String tokenValue = jwtProvider.getEmail(token);
        return tokenValue.equals(email);
    }

    // Each login opens its own session, so devices don't sign each other out
    @Transactional
    public LoginResponse createAuthTokens(Long id) {
        return issueAuthTokens(id, UUID.randomUUID().toString());
    }

    private LoginResponse issueAuthTokens(Long id, String sessionId) {
        String grantType = GrantType.BEARER.getType();
        String accessToken = jwtProvider.createAccessToken(id, sessionId);
        String refreshToken = jwtProvider.createRefreshToken(id, sessionId);
        Instant accessTokenExpiredAt = clock.instant().plus(Duration.ofMinutes(ACCESS_TOKEN_VALID_MINUTE));
        Instant refreshTokenExpiredAt = clock.instant().plus(Duration.ofMinutes(REFRESH_TOKEN_VALID_MINUTE));

        refreshSessionRepository.save(id, sessionId, refreshToken);

        return LoginResponse.builder()
                .grantType(grantType)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiredAt(accessTokenExpiredAt)
                .refreshTokenExpiredAt(refreshTokenExpiredAt)
                .build();
    }

    public Long logout(UserEntity user, String sessionId) {
        if (sessionId != null) {
            refreshSessionRepository.delete(user.getId(), sessionId);
        }
        return user.getId();
    }

    public void revokeAllSessions(Long userId) {
        refreshSessionRepository.deleteAll(userId);
    }

    public void revokeOtherSessions(Long userId, String currentSessionId) {
        refreshSessionRepository.deleteAllExcept(userId, currentSessionId);
    }

    // Only the session's latest refresh token is accepted
    @Transactional
    public LoginResponse reissueToken(ReissueTokenRequest request) {
        TokenClaims claims = jwtProvider.parseRefreshToken(request.refreshToken());
        String storedToken = refreshSessionRepository.findToken(claims.sessionId())
                .orElseThrow(() -> new CustomException(ErrorCode.EXPIRED_JWT_TOKEN));
        if (!storedToken.equals(request.refreshToken())) {
            throw new CustomException(ErrorCode.EXPIRED_JWT_TOKEN);
        }

        return issueAuthTokens(claims.userId(), claims.sessionId());
    }

}
