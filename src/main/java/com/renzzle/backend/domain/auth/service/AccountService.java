package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.domain.auth.api.request.LoginRequest;
import com.renzzle.backend.domain.auth.api.request.SignupRequest;
import com.renzzle.backend.domain.auth.api.response.LoginResponse;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final Clock clock;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AuthService authService;

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
    public LoginResponse signUp(SignupRequest request) {
        if(!authService.verifyAuthVerityToken(request.authVerityToken(), request.email())) {
            throw new CustomException(ErrorCode.INVALID_AUTH_VERITY_TOKEN);
        }

        UserEntity user = createNewUser(request.email(), request.password(), request.nickname(), request.deviceId());

        return authService.createAuthTokens(user.getId());
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

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Optional<UserEntity> user = userRepository.findByEmail(request.email());

        if(user.isEmpty())
            throw new CustomException(ErrorCode.INVALID_EMAIL);

        if(!passwordEncoder.matches(request.password(), user.get().getPassword()))
            throw new CustomException(ErrorCode.INVALID_PASSWORD);

        user.get().updateLastAccessedAt(clock.instant());

        long userId = user.get().getId();

        return authService.createAuthTokens(userId);
    }

}
