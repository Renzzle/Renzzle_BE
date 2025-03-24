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
    private JavaMailSender javaMailSender;
    @Mock
    private SpringTemplateEngine templateEngine;
    @Mock
    private EmailRedisRepository emailRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private AuthService authService;

    @InjectMocks
    private EmailService emailService;

    private final String FIXED_TIME = "2025-03-20T10:00:00Z";
    private final String FIXED_TIME_BEFORE_5MIN = "2025-03-20T09:55:00Z";
    private final String FIXED_TIME_BEFORE_5MIN_1SEC = "2025-03-20T09:54:59Z";

    @BeforeEach
    public void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse(FIXED_TIME));
    }

    @Test
    void sendCode_shouldReturnRequestCountAndSendEmail() throws MessagingException, IOException {
        // Given
        AuthEmailRequest request = new AuthEmailRequest("test@example.com");
        when(accountService.isDuplicatedEmail(request.email())).thenReturn(false);
        when(emailRepository.findById(request.email())).thenReturn(Optional.empty());

        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("verification_email"), any(Context.class))).thenAnswer(
                invocation -> {
                    Context context = invocation.getArgument(1);
                    return context.getVariable("verificationCode");
                }
        );

        // When
        AuthEmailResponse response = emailService.sendCode(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.requestCount()).isEqualTo(1);
        assertThat(mimeMessage.getAllRecipients()[0].toString()).isEqualTo("test@example.com");
        assertThat(mimeMessage.getSubject()).isEqualTo("[Renzzle] Email Verification");
        String emailContent = (String) mimeMessage.getContent();
        assertThat(emailContent).matches(".*\\b\\d{6}\\b.*"); // assert six-digit string
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendCode_WithDuplicatedEmail_ShouldThrowException() {
        // Given
        AuthEmailRequest request = new AuthEmailRequest("duplicate@example.com");
        when(accountService.isDuplicatedEmail(request.email())).thenReturn(true);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            emailService.sendCode(request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void sendCode_ExceedRequestCount_ShouldThrowException() {
        // Given
        AuthEmailRequest request = new AuthEmailRequest("test@example.com");
        AuthEmailEntity emailEntity = new AuthEmailEntity("test@example.com", "123456", EMAIL_VERIFICATION_LIMIT, FIXED_TIME);
        when(emailRepository.findById(request.email())).thenReturn(Optional.of(emailEntity));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            emailService.sendCode(request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXCEED_EMAIL_AUTH_REQUEST);
    }

    @Test
    void confirmCode_ShouldReturnAuthVerityToken() {
        // Given
        ConfirmCodeRequest request = new ConfirmCodeRequest("test@example.com", "123456");
        AuthEmailEntity emailEntity = new AuthEmailEntity("test@example.com", "123456", 1, FIXED_TIME_BEFORE_5MIN);
        when(emailRepository.findById(request.email())).thenReturn(Optional.of(emailEntity));
        when(authService.createAuthVerityToken(request.email())).thenReturn("authToken123");

        // When
        ConfirmCodeResponse response = emailService.confirmCode(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.authVerityToken()).isEqualTo("authToken123");
    }

    @Test
    void confirmCode_WithExpiredCode_ShouldThrowException() {
        // Given
        ConfirmCodeRequest request = new ConfirmCodeRequest("test@example.com", "123456");
        AuthEmailEntity emailEntity = new AuthEmailEntity("test@example.com", "123456", 1, FIXED_TIME_BEFORE_5MIN_1SEC);
        when(emailRepository.findById(request.email())).thenReturn(Optional.of(emailEntity));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            emailService.confirmCode(request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_EMAIL_AUTH_CODE);
    }

    @Test
    void confirmCode_WithWrongCode_ShouldThrowException() {
        // Given
        ConfirmCodeRequest request = new ConfirmCodeRequest("test@example.com", "654321");
        AuthEmailEntity emailEntity = new AuthEmailEntity("test@example.com", "123456", 1, FIXED_TIME_BEFORE_5MIN);
        when(emailRepository.findById(request.email())).thenReturn(Optional.of(emailEntity));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            emailService.confirmCode(request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_EMAIL_AUTH_CODE);
    }

}
