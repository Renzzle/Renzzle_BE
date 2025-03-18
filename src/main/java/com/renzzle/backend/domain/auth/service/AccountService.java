package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.domain.auth.api.response.LoginResponse;
import com.renzzle.backend.domain.auth.dao.RefreshTokenRedisRepository;
import com.renzzle.backend.domain.auth.domain.GrantType;
import com.renzzle.backend.domain.auth.domain.RefreshTokenEntity;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import static com.renzzle.backend.domain.auth.service.JwtProvider.ACCESS_TOKEN_VALID_MINUTE;
import static com.renzzle.backend.domain.auth.service.JwtProvider.REFRESH_TOKEN_VALID_MINUTE;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final RefreshTokenRedisRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String createAuthVerityToken(String email) {
        return jwtProvider.createAuthVerityToken(email);
    }

    public boolean verifyAuthVerityToken(String token, String email) {
        String tokenValue = jwtProvider.getEmail(token);
        return tokenValue.equals(email);
    }

    @Transactional(readOnly = true)
    public boolean isDuplicatedEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean isDuplicateNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    @Transactional(readOnly = true)
    public boolean isDuplicateSignUp(String deviceId) {
        return userRepository.existsByDeviceId(deviceId);
    }

    @Transactional
    public UserEntity createNewUser(String email, String password, String nickname, String deviceId) {
        // validate email
        if(isDuplicatedEmail(email))
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        // validate nickname
        if(isDuplicateNickname(nickname))
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        // validate duplicate sign-up
        if(isDuplicateSignUp(deviceId))
            throw new CustomException(ErrorCode.DUPLICATE_DEVICE);

        String encodedPassword = passwordEncoder.encode(password);

        UserEntity user = UserEntity.builder()
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .deviceId(deviceId)
                .build();
        return userRepository.save(user);
    }

    public LoginResponse createAuthTokens(Long id) {
        String grantType = GrantType.BEARER.getType();
        String accessToken = jwtProvider.createAccessToken(id);
        String refreshToken = jwtProvider.createRefreshToken(id);
        Instant accessTokenExpiredAt = Instant.now().plus(Duration.ofMinutes(ACCESS_TOKEN_VALID_MINUTE));
        Instant refreshTokenExpiredAt = Instant.now().plus(Duration.ofMinutes(REFRESH_TOKEN_VALID_MINUTE));

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

    @Transactional(readOnly = true)
    public Long verifyLoginInfo(String email, String password) {
        Optional<UserEntity> user = userRepository.findByEmail(email);

        if(user.isEmpty())
            throw new CustomException(ErrorCode.INVALID_EMAIL);

        if(!passwordEncoder.matches(password, user.get().getPassword()))
            throw new CustomException(ErrorCode.INVALID_PASSWORD);

        return user.get().getId();
    }

    public Long deleteRefreshToken(Long id) {
        refreshTokenRepository.deleteById(id);
        return id;
    }

    public boolean verifyRefreshToken(String token) {
        Long userId = jwtProvider.getUserId(token);
        Optional<RefreshTokenEntity> tokenEntity = refreshTokenRepository.findById(userId);

        return tokenEntity.isPresent();
    }

}
