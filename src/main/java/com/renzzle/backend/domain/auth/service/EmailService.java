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
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailService {

    public static final int EMAIL_CODE_VALID_SECOND = 5 * 60; // 5 minute
    public static final int EMAIL_VERIFICATION_LIMIT = 5;

    private final Clock clock;
    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;
    private final EmailRedisRepository emailRepository;
    private final AccountService accountService;
    private final AuthService authService;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Transactional
    public AuthEmailResponse sendCode(AuthEmailRequest request) {
        if(accountService.isDuplicatedEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        int count = getRequestCount(request.email());
        if(count >= EMAIL_VERIFICATION_LIMIT) {
            throw new CustomException(ErrorCode.EXCEED_EMAIL_AUTH_REQUEST);
        }

        String code = sendAuthEmail(request.email());
        saveConfirmCode(request.email(), code, count);

        return AuthEmailResponse
                .builder()
                .requestCount(count + 1)
                .build();
    }

    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            int digit = random.nextInt(10);
            code.append(digit);
        }

        return code.toString();
    }

    private String sendAuthEmail(String address) {
        MimeMessage message = javaMailSender.createMimeMessage();
        String code = generateRandomCode();

        try {
            message.setFrom(senderEmail);
            message.setRecipients(MimeMessage.RecipientType.TO, address);
            message.setSubject("[Renzzle] Email Verification");

            Context context = new Context();
            context.setVariable("verificationCode", code);
            String htmlContent = templateEngine.process("verification_email", context);

            message.setText(htmlContent, "UTF-8", "html");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

        javaMailSender.send(message);

        return code;
    }

    private int getRequestCount(String address) {
        int count = 0;
        Optional<AuthEmailEntity> emailEntity = emailRepository.findById(address);
        if(emailEntity.isPresent()) {
            count = emailEntity.get().count();
        }
        return count;
    }

    private void saveConfirmCode(String address, String code, int count) {
        AuthEmailEntity result = AuthEmailEntity
                .builder()
                .email(address)
                .code(code)
                .count(count + 1)
                .issuedAt(clock.instant().toString())
                .build();
        emailRepository.save(result);
    }

    @Transactional(readOnly = true)
    public ConfirmCodeResponse confirmCode(ConfirmCodeRequest request) {
        boolean isCorrect = verifyCode(request.email(), request.code());
        if(!isCorrect) throw new CustomException(ErrorCode.INVALID_EMAIL_AUTH_CODE);

        String authVerityToken = authService.createAuthVerityToken(request.email());

        return ConfirmCodeResponse
                .builder()
                .authVerityToken(authVerityToken)
                .build();
    }

    private boolean verifyCode(String address, String code) {
        Optional<AuthEmailEntity> emailEntity = emailRepository.findById(address);

        if(emailEntity.isPresent()) {
            Instant now = clock.instant();
            Instant issuedAt = Instant.parse(emailEntity.get().issuedAt());

            Duration duration = Duration.between(issuedAt, now);
            if (duration.toSeconds() > EMAIL_CODE_VALID_SECOND) {
                return false;
            }
        }

        return emailEntity.map(authEmailEntity ->
                authEmailEntity.code().equals(code)).orElse(false);
    }

}
