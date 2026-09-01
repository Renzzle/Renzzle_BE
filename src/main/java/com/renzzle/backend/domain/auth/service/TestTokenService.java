package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.domain.auth.api.request.TestTokenRequest;
import com.renzzle.backend.domain.auth.api.response.TestTokenResponse;
import com.renzzle.backend.domain.auth.domain.GrantType;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import static com.renzzle.backend.domain.auth.service.JwtProvider.TEST_ACCESS_TOKEN_VALID_MINUTE;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "docs.enabled", havingValue = "true")
public class TestTokenService {

    private final Clock clock;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Same credential check as login, but issues a longer lived access token.
    // No refresh token is stored, so the login session of the account is left untouched.
    @Transactional(readOnly = true)
    public TestTokenResponse createTestToken(TestTokenRequest request) {
        Optional<UserEntity> user = userRepository.findByEmail(request.email());

        if (user.isEmpty())
            throw new CustomException(ErrorCode.INVALID_EMAIL);

        if (!passwordEncoder.matches(request.password(), user.get().getPassword()))
            throw new CustomException(ErrorCode.INVALID_PASSWORD);

        return TestTokenResponse.builder()
                .grantType(GrantType.BEARER.getType())
                .accessToken(jwtProvider.createTestAccessToken(user.get().getId()))
                .accessTokenExpiredAt(clock.instant().plus(Duration.ofMinutes(TEST_ACCESS_TOKEN_VALID_MINUTE)))
                .build();
    }

}
