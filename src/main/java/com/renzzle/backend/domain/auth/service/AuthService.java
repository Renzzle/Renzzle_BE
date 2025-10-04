package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.domain.auth.api.request.ReissueTokenRequest;
import com.renzzle.backend.domain.auth.api.response.LoginResponse;
import com.renzzle.backend.domain.auth.dao.RefreshTokenRedisRepository;
import com.renzzle.backend.domain.auth.domain.GrantType;
import com.renzzle.backend.domain.auth.domain.RefreshTokenEntity;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import static com.renzzle.backend.domain.auth.service.JwtProvider.ACCESS_TOKEN_VALID_MINUTE;
import static com.renzzle.backend.domain.auth.service.JwtProvider.REFRESH_TOKEN_VALID_MINUTE;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final Clock clock;
    private final RefreshTokenRedisRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    public String createAuthVerityToken(String email) {
        return jwtProvider.createAuthVerityToken(email);
    }

    public boolean verifyAuthVerityToken(String token, String email) {
        String tokenValue = jwtProvider.getEmail(token);
        return tokenValue.equals(email);
    }

    @Transactional
    public LoginResponse createAuthTokens(Long id) {
        String grantType = GrantType.BEARER.getType();
        String accessToken = jwtProvider.createAccessToken(id);
        String refreshToken = jwtProvider.createRefreshToken(id);
        Instant accessTokenExpiredAt = clock.instant().plus(Duration.ofMinutes(ACCESS_TOKEN_VALID_MINUTE));
        Instant refreshTokenExpiredAt = clock.instant().plus(Duration.ofMinutes(REFRESH_TOKEN_VALID_MINUTE));

        refreshTokenRepository.save(RefreshTokenEntity.builder()
                .id(id)
                .token(refreshToken)
                .build());

        return LoginResponse.builder()
                .grantType(grantType)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiredAt(accessTokenExpiredAt)
                .refreshTokenExpiredAt(refreshTokenExpiredAt)
                .build();
    }

    @Transactional
    public Long deleteRefreshToken(UserEntity user) {
        refreshTokenRepository.deleteById(user.getId());
        return user.getId();
    }

    @Transactional
    public LoginResponse reissueToken(ReissueTokenRequest request) {
        Long userId = jwtProvider.getUserId(request.refreshToken());
        Optional<RefreshTokenEntity> tokenEntity = refreshTokenRepository.findById(userId);

        if(tokenEntity.isEmpty())
            throw new CustomException(ErrorCode.EXPIRED_JWT_TOKEN);

        return createAuthTokens(userId);
    }

    private boolean verifyRefreshToken(String token) {
        Long userId = jwtProvider.getUserId(token);
        Optional<RefreshTokenEntity> tokenEntity = refreshTokenRepository.findById(userId);

        return tokenEntity.isPresent();
    }

}
