package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.domain.auth.api.request.AuthEmailRequest;
import com.renzzle.backend.domain.auth.api.request.ConfirmCodeRequest;
import com.renzzle.backend.domain.auth.api.response.AuthEmailResponse;
import com.renzzle.backend.domain.auth.api.response.ConfirmCodeResponse;
import com.renzzle.backend.domain.auth.dao.EmailRedisRepository;
import com.renzzle.backend.domain.auth.domain.AuthEmailEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Properties;

import static com.renzzle.backend.domain.auth.service.EmailService.EMAIL_VERIFICATION_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

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

    @InjectMocks
    private EmailService emailService;

    private final String FIXED_TIME = "2025-03-20T10:00:00Z";

    @BeforeEach
    public void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse(FIXED_TIME));
    }

    @Test
    public void sendCode_ShouldReturnRequestCountAndSendEmail() throws MessagingException, IOException {
        // given
        AuthEmailRequest request = new AuthEmailRequest("test@example.com");
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
    public void sendCode_WithDuplicatedEmail_ShouldThrowException() {
        // given
        AuthEmailRequest request = new AuthEmailRequest("duplicate@example.com");
        when(accountService.isDuplicatedEmail(request.email())).thenReturn(true);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            emailService.sendCode(request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    public void sendCode_ExceedRequestCount_ShouldThrowException() {
        // given
        AuthEmailRequest request = new AuthEmailRequest("test@example.com");
        AuthEmailEntity emailEntity = new AuthEmailEntity("test@example.com", "123456", EMAIL_VERIFICATION_LIMIT, FIXED_TIME);
        when(emailRepository.findById(request.email())).thenReturn(Optional.of(emailEntity));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            emailService.sendCode(request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXCEED_EMAIL_AUTH_REQUEST);
    }

    @Test
    public void confirmCode_ShouldReturnAuthVerityToken() {
        // given
        ConfirmCodeRequest request = new ConfirmCodeRequest("test@example.com", "123456");
        String fixedTimeBefore5min = Instant.parse(FIXED_TIME).minusSeconds(60 * 5).toString();
        AuthEmailEntity emailEntity = new AuthEmailEntity("test@example.com", "123456", 1, fixedTimeBefore5min);
        when(emailRepository.findById(request.email())).thenReturn(Optional.of(emailEntity));
        when(authService.createAuthVerityToken(request.email())).thenReturn("authToken123");

        // when
        ConfirmCodeResponse response = emailService.confirmCode(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.authVerityToken()).isEqualTo("authToken123");
    }

    @Test
    public void confirmCode_WithExpiredCode_ShouldThrowException() {
        // given
        ConfirmCodeRequest request = new ConfirmCodeRequest("test@example.com", "123456");
        String fixedTimeBefore5min1sec = Instant.parse(FIXED_TIME).minusSeconds(60 * 5 + 1).toString();
        AuthEmailEntity emailEntity = new AuthEmailEntity("test@example.com", "123456", 1, fixedTimeBefore5min1sec);
        when(emailRepository.findById(request.email())).thenReturn(Optional.of(emailEntity));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            emailService.confirmCode(request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_EMAIL_AUTH_CODE);
    }

    @Test
    public void confirmCode_WithWrongCode_ShouldThrowException() {
        // given
        ConfirmCodeRequest request = new ConfirmCodeRequest("test@example.com", "654321");
        String fixedTimeBefore5min = Instant.parse(FIXED_TIME).minusSeconds(60 * 5).toString();
        AuthEmailEntity emailEntity = new AuthEmailEntity("test@example.com", "123456", 1, fixedTimeBefore5min);
        when(emailRepository.findById(request.email())).thenReturn(Optional.of(emailEntity));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            emailService.confirmCode(request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_EMAIL_AUTH_CODE);
    }

}
