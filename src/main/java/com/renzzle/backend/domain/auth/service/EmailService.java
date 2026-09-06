package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.domain.auth.api.request.AuthEmailRequest;
import com.renzzle.backend.domain.auth.api.request.ConfirmCodeRequest;
import com.renzzle.backend.domain.auth.api.response.AuthEmailResponse;
import com.renzzle.backend.domain.auth.api.response.ConfirmCodeResponse;
import com.renzzle.backend.domain.auth.dao.EmailRedisRepository;
import com.renzzle.backend.domain.auth.domain.AuthEmailEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
public class EmailService {

    public static final int EMAIL_CODE_VALID_SECOND = 5 * 60; // 5 minute
    public static final int EMAIL_VERIFICATION_LIMIT = 5;
    public static final int EMAIL_CODE_ATTEMPT_LIMIT = 50;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Clock clock;
    private final EmailRedisRepository emailRepository;
    private final AccountService accountService;
    private final AuthService authService;
    private final EmailSender emailSender;

    private String generateRandomCode() {
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            int digit = RANDOM.nextInt(10);
            code.append(digit);
        }

        return code.toString();
    }

    @Transactional
    public AuthEmailResponse sendCode(AuthEmailRequest request) {
        if(accountService.isDuplicatedEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        return sendCodeTo(request.email(), emailSender::sendAuthEmail);
    }

    @Transactional
    public AuthEmailResponse sendPasswordResetCode(AuthEmailRequest request) {
        if(!accountService.isDuplicatedEmail(request.email())) {
            throw new CustomException(ErrorCode.INVALID_EMAIL);
        }

        return sendCodeTo(request.email(), emailSender::sendPasswordResetEmail);
    }

    private AuthEmailResponse sendCodeTo(String email, BiConsumer<String, String> emailConsumer) {
        int count = getRequestCount(email);
        if(count >= EMAIL_VERIFICATION_LIMIT) {
            throw new CustomException(ErrorCode.EXCEED_EMAIL_AUTH_REQUEST);
        }

        String code = generateRandomCode();
        emailConsumer.accept(email, code);

        saveConfirmCode(email, code, count);

        return AuthEmailResponse
                .builder()
                .requestCount(count + 1)
                .build();
    }

    private int getRequestCount(String address) {
        int count = 0;
        Optional<AuthEmailEntity> emailEntity = emailRepository.findById(address);
        if(emailEntity.isPresent()) {
            count = emailEntity.get().count();
        }
        return count;
    }

    // Issuing a new code replaces the previous one and clears its attempt count
    private void saveConfirmCode(String address, String code, int count) {
        AuthEmailEntity result = AuthEmailEntity
                .builder()
                .email(address)
                .code(code)
                .count(count + 1)
                .attemptCount(0)
                .verified(false)
                .issuedAt(clock.instant().toString())
                .build();
        emailRepository.save(result);
    }

    @Transactional
    public ConfirmCodeResponse confirmCode(ConfirmCodeRequest request) {
        AuthEmailEntity emailEntity = emailRepository.findById(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_EMAIL_AUTH_CODE));

        // A code is single use, so an already verified one is treated the same as a wrong one
        if(emailEntity.verified()) {
            throw new CustomException(ErrorCode.INVALID_EMAIL_AUTH_CODE);
        }

        // Guessing is cut off once the limit is hit; the caller has to request a new code.
        // The record itself is kept so that the send count cannot be reset by burning attempts.
        if(emailEntity.attemptCount() >= EMAIL_CODE_ATTEMPT_LIMIT) {
            throw new CustomException(ErrorCode.EXCEED_EMAIL_AUTH_ATTEMPT);
        }

        if(isExpired(emailEntity) || !request.code().equals(emailEntity.code())) {
            emailRepository.save(emailEntity.increaseAttemptCount());
            throw new CustomException(ErrorCode.INVALID_EMAIL_AUTH_CODE);
        }

        emailRepository.save(emailEntity.verify());

        String authVerityToken = authService.createAuthVerityToken(request.email());

        return ConfirmCodeResponse
                .builder()
                .authVerityToken(authVerityToken)
                .build();
    }

    private boolean isExpired(AuthEmailEntity emailEntity) {
        Duration duration = Duration.between(Instant.parse(emailEntity.issuedAt()), clock.instant());
        return duration.toSeconds() > EMAIL_CODE_VALID_SECOND;
    }

}
