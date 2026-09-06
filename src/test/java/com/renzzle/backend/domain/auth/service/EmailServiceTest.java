package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.domain.auth.api.request.AuthEmailRequest;
import com.renzzle.backend.domain.auth.api.request.ConfirmCodeRequest;
import com.renzzle.backend.domain.auth.api.response.AuthEmailResponse;
import com.renzzle.backend.domain.auth.api.response.ConfirmCodeResponse;
import com.renzzle.backend.domain.auth.dao.EmailRedisRepository;
import com.renzzle.backend.domain.auth.domain.AuthEmailEntity;
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

import static com.renzzle.backend.domain.auth.service.EmailService.EMAIL_CODE_ATTEMPT_LIMIT;
import static com.renzzle.backend.domain.auth.service.EmailService.EMAIL_VERIFICATION_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    Clock clock;
    @Mock
    private EmailSender emailSender;
    @Mock
    private EmailRedisRepository emailRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private AuthService authService;
    @Captor
    private ArgumentCaptor<AuthEmailEntity> emailEntityCaptor;

    @InjectMocks
    private EmailService emailService;

    private static final String FIXED_TIME = "2025-03-20T10:00:00Z";
    private static final String EMAIL = "test@example.com";
    private static final String CODE = "123456";

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse(FIXED_TIME));
    }

    private AuthEmailEntity.AuthEmailEntityBuilder validEmailEntity() {
        return AuthEmailEntity.builder()
                .email(EMAIL)
                .code(CODE)
                .count(1)
                .attemptCount(0)
                .verified(false)
                .issuedAt(Instant.parse(FIXED_TIME).minusSeconds(60 * 5).toString());
    }

    @Test
    void sendCode_ShouldReturnRequestCountAndSendEmail() {
        // given
        AuthEmailRequest request = new AuthEmailRequest(EMAIL);
        when(accountService.isDuplicatedEmail(request.email())).thenReturn(false);
        when(emailRepository.findById(request.email())).thenReturn(Optional.empty());

        // when
        AuthEmailResponse response = emailService.sendCode(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.requestCount()).isEqualTo(1);
        verify(emailSender, times(1)).sendAuthEmail(any(String.class), any(String.class));
    }

    @Test
    void sendCode_WithDuplicatedEmail_ShouldThrowException() {
        // given
        AuthEmailRequest request = new AuthEmailRequest("duplicate@example.com");
        when(accountService.isDuplicatedEmail(request.email())).thenReturn(true);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> emailService.sendCode(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void sendCode_ShouldResetAttemptCount() {
        // given
        AuthEmailRequest request = new AuthEmailRequest(EMAIL);
        when(accountService.isDuplicatedEmail(request.email())).thenReturn(false);
        when(emailRepository.findById(request.email()))
                .thenReturn(Optional.of(validEmailEntity().attemptCount(EMAIL_CODE_ATTEMPT_LIMIT).build()));

        // when
        emailService.sendCode(request);

        // then
        verify(emailRepository).save(emailEntityCaptor.capture());
        assertThat(emailEntityCaptor.getValue().attemptCount()).isZero();
        assertThat(emailEntityCaptor.getValue().verified()).isFalse();
    }

    @Test
    void sendPasswordResetCode_ShouldSendEmail_WhenEmailExists() {
        AuthEmailRequest request = new AuthEmailRequest("registered@example.com");
        when(accountService.isDuplicatedEmail(request.email())).thenReturn(true);
        when(emailRepository.findById(request.email())).thenReturn(Optional.empty());

        AuthEmailResponse response = emailService.sendPasswordResetCode(request);

        assertThat(response.requestCount()).isEqualTo(1);
        verify(emailSender).sendPasswordResetEmail(eq(request.email()), any(String.class));
    }

    @Test
    void sendPasswordResetCode_ShouldThrowException_WhenEmailDoesNotExist() {
        AuthEmailRequest request = new AuthEmailRequest("unknown@example.com");
        when(accountService.isDuplicatedEmail(request.email())).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class,
                () -> emailService.sendPasswordResetCode(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_EMAIL);
        verifyNoInteractions(emailSender);
    }

    @Test
    void sendCode_ExceedRequestCount_ShouldThrowException() {
        // given
        AuthEmailRequest request = new AuthEmailRequest(EMAIL);
        when(emailRepository.findById(request.email()))
                .thenReturn(Optional.of(validEmailEntity().count(EMAIL_VERIFICATION_LIMIT).build()));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> emailService.sendCode(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXCEED_EMAIL_AUTH_REQUEST);
    }

    @Test
    void confirmCode_ShouldReturnAuthVerityToken() {
        // given
        ConfirmCodeRequest request = new ConfirmCodeRequest(EMAIL, CODE);
        when(emailRepository.findById(request.email())).thenReturn(Optional.of(validEmailEntity().build()));
        when(authService.createAuthVerityToken(request.email())).thenReturn("authToken123");

        // when
        ConfirmCodeResponse response = emailService.confirmCode(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.authVerityToken()).isEqualTo("authToken123");
    }

    @Test
    void confirmCode_ShouldMarkCodeAsVerified() {
        // given
        ConfirmCodeRequest request = new ConfirmCodeRequest(EMAIL, CODE);
        when(emailRepository.findById(request.email())).thenReturn(Optional.of(validEmailEntity().build()));

        // when
        emailService.confirmCode(request);

        // then
        verify(emailRepository).save(emailEntityCaptor.capture());
        assertThat(emailEntityCaptor.getValue().verified()).isTrue();
        assertThat(emailEntityCaptor.getValue().count()).isEqualTo(1);
    }

    @Test
    void confirmCode_WithAlreadyVerifiedCode_ShouldThrowException() {
        // given
        ConfirmCodeRequest request = new ConfirmCodeRequest(EMAIL, CODE);
        when(emailRepository.findById(request.email())).thenReturn(Optional.of(validEmailEntity().verified(true).build()));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> emailService.confirmCode(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_EMAIL_AUTH_CODE);
        verify(emailRepository, never()).save(any(AuthEmailEntity.class));
    }

    @Test
    void confirmCode_WithUnknownEmail_ShouldThrowException() {
        // given
        ConfirmCodeRequest request = new ConfirmCodeRequest(EMAIL, CODE);
        when(emailRepository.findById(request.email())).thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> emailService.confirmCode(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_EMAIL_AUTH_CODE);
    }

    @Test
    void confirmCode_WithExpiredCode_ShouldThrowException() {
        // given
        ConfirmCodeRequest request = new ConfirmCodeRequest(EMAIL, CODE);
        String fixedTimeBefore5min1sec = Instant.parse(FIXED_TIME).minusSeconds(60 * 5 + 1).toString();
        when(emailRepository.findById(request.email()))
                .thenReturn(Optional.of(validEmailEntity().issuedAt(fixedTimeBefore5min1sec).build()));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> emailService.confirmCode(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_EMAIL_AUTH_CODE);
    }

    @Test
    void confirmCode_WithWrongCode_ShouldThrowException() {
        // given
        ConfirmCodeRequest request = new ConfirmCodeRequest(EMAIL, "654321");
        when(emailRepository.findById(request.email())).thenReturn(Optional.of(validEmailEntity().build()));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> emailService.confirmCode(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_EMAIL_AUTH_CODE);
    }

    @Test
    void confirmCode_WithWrongCode_ShouldIncreaseAttemptCount() {
        // given
        ConfirmCodeRequest request = new ConfirmCodeRequest(EMAIL, "654321");
        when(emailRepository.findById(request.email())).thenReturn(Optional.of(validEmailEntity().attemptCount(3).build()));

        // when
        assertThrows(CustomException.class, () -> emailService.confirmCode(request));

        // then
        verify(emailRepository).save(emailEntityCaptor.capture());
        assertThat(emailEntityCaptor.getValue().attemptCount()).isEqualTo(4);
        assertThat(emailEntityCaptor.getValue().count()).isEqualTo(1);
    }

    @Test
    void confirmCode_ExceedAttemptLimit_ShouldThrowException() {
        // given
        ConfirmCodeRequest request = new ConfirmCodeRequest(EMAIL, CODE);
        when(emailRepository.findById(request.email()))
                .thenReturn(Optional.of(validEmailEntity().attemptCount(EMAIL_CODE_ATTEMPT_LIMIT).build()));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> emailService.confirmCode(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXCEED_EMAIL_AUTH_ATTEMPT);
        verify(emailRepository, never()).save(any(AuthEmailEntity.class));
        verifyNoInteractions(authService);
    }

}
