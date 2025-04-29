package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.domain.auth.api.request.LoginRequest;
import com.renzzle.backend.domain.auth.api.request.SignupRequest;
import com.renzzle.backend.domain.auth.api.response.LoginResponse;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private Clock clock;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthService authService;

    @InjectMocks
    private AccountService accountService;

    private final String email = "test@example.com";
    private final String password = "password123";
    private final String nickname = "tester";
    private final String authVerityToken = "auth-token";
    private final String deviceId = "device-123";

    private SignupRequest validSignupRequest;
    private LoginRequest validLoginRequest;

    @BeforeEach
    public void setup() {
        validSignupRequest = new SignupRequest(email, password, nickname, authVerityToken, deviceId);
        validLoginRequest = new LoginRequest(email, password);
    }

    @Test
    public void signUp_ShouldCreateUserAndReturnTokens_WhenValid() {
        // given
        when(authService.verifyAuthVerityToken(authVerityToken, email)).thenReturn(true);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.existsByNickname(nickname)).thenReturn(false);
        when(userRepository.existsByDeviceId(deviceId)).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(i -> UserEntity.builder().id(1L).build());
        when(authService.createAuthTokens(anyLong())).thenReturn(mock(LoginResponse.class));

        // when
        LoginResponse response = accountService.signUp(validSignupRequest);

        // then
        assertNotNull(response);
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    public void signUp_ShouldThrowException_WhenEmailAlreadyExists() {
        when(authService.verifyAuthVerityToken(authVerityToken, email)).thenReturn(true);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        CustomException ex = assertThrows(CustomException.class, () -> {
            accountService.signUp(validSignupRequest);
        });

        assertEquals(ErrorCode.DUPLICATE_EMAIL, ex.getErrorCode());
    }

    @Test
    public void signUp_ShouldThrowException_WhenInvalidAuthToken() {
        when(authService.verifyAuthVerityToken(authVerityToken, email)).thenReturn(false);

        CustomException ex = assertThrows(CustomException.class, () -> {
            accountService.signUp(validSignupRequest);
        });

        assertEquals(ErrorCode.INVALID_AUTH_VERITY_TOKEN, ex.getErrorCode());
    }

    @Test
    public void login_ShouldReturnTokens_WhenCredentialsAreValid() {
        UserEntity user = UserEntity.builder()
                .email(email)
                .password(new BCryptPasswordEncoder().encode(password))
                .nickname(nickname)
                .deviceId(deviceId)
                .id(1L)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(authService.createAuthTokens(1L)).thenReturn(mock(LoginResponse.class));

        LoginResponse response = accountService.login(validLoginRequest);

        assertNotNull(response);
    }

    @Test
    public void login_ShouldThrowException_WhenEmailNotFound() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> {
            accountService.login(validLoginRequest);
        });

        assertEquals(ErrorCode.INVALID_EMAIL, ex.getErrorCode());
    }

    @Test
    public void login_ShouldThrowException_WhenPasswordMismatch() {
        UserEntity user = UserEntity.builder()
                .email(email)
                .password(new BCryptPasswordEncoder().encode("wrong-password"))
                .nickname(nickname)
                .deviceId(deviceId)
                .id(1L)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        CustomException ex = assertThrows(CustomException.class, () -> {
            accountService.login(validLoginRequest);
        });

        assertEquals(ErrorCode.INVALID_PASSWORD, ex.getErrorCode());
    }

}
