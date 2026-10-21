package com.renzzle.backend.domain.payment.service;

import com.apple.itunes.storekit.model.Environment;
import com.apple.itunes.storekit.verification.SignedDataVerifier;
import com.apple.itunes.storekit.verification.VerificationException;
import com.apple.itunes.storekit.verification.VerificationStatus;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Set;

// Checks that an App Store server notification was signed by Apple: its certificate chain must lead to Apple Root
// CA - G3 and its bundle ID, environment and (in production) app Apple ID must be ours. Anyone can call the
// notification URL, so nothing in a notification is used before this passes
@Slf4j
@Component
public class AppleNotificationVerifier {

    private static final String ROOT_CERTIFICATE = "apple/AppleRootCA-G3.cer";
    private static final String SANDBOX_ENVIRONMENT = "Sandbox";

    // Null when the app Apple ID is not configured, which the production verifier requires
    private final SignedDataVerifier productionVerifier;
    private final SignedDataVerifier sandboxVerifier;

    @Autowired
    public AppleNotificationVerifier(
            @Value("${iap.apple.bundle-id}") String bundleId,
            @Value("${iap.apple.app-apple-id:}") String appAppleId
    ) {
        this(createProductionVerifier(bundleId, parseAppAppleId(appAppleId)),
                createVerifier(bundleId, null, Environment.SANDBOX));
    }

    // Tests pass verifiers directly, since a valid signature needs Apple's private key
    AppleNotificationVerifier(SignedDataVerifier productionVerifier, SignedDataVerifier sandboxVerifier) {
        this.productionVerifier = productionVerifier;
        this.sandboxVerifier = sandboxVerifier;
    }

    // The environment named in the payload only picks the verifier, which then checks it against the signed data
    public void verify(String signedPayload, String environment) {
        SignedDataVerifier verifier = SANDBOX_ENVIRONMENT.equalsIgnoreCase(environment)
                ? sandboxVerifier
                : productionVerifier;
        if (verifier == null) {
            log.error("Production App Store notification refused: iap.apple.app-apple-id is not set");
            throw new CustomException(ErrorCode.STORE_API_FAILED);
        }

        try {
            verifier.verifyAndDecodeNotification(signedPayload);
        } catch (VerificationException e) {
            if (e.getStatus() == VerificationStatus.RETRYABLE_VERIFICATION_FAILURE) {
                // Certificate revocation could not be checked right now; a non-200 answer makes Apple send it again
                log.warn("Apple notification signature check failed for now. environment={}", environment, e);
                throw new CustomException(ErrorCode.STORE_API_FAILED);
            }
            log.warn("Apple notification rejected. status={}, environment={}", e.getStatus(), environment);
            throw new CustomException(ErrorCode.INVALID_STORE_NOTIFICATION);
        } catch (RuntimeException e) {
            log.warn("Apple notification rejected.", e);
            throw new CustomException(ErrorCode.INVALID_STORE_NOTIFICATION);
        }
    }

    private static SignedDataVerifier createProductionVerifier(String bundleId, Long appAppleId) {
        if (appAppleId == null) {
            log.error("iap.apple.app-apple-id is not set, so production App Store notifications will be refused");
            return null;
        }
        return createVerifier(bundleId, appAppleId, Environment.PRODUCTION);
    }

    // Online checks: certificate revocation (OCSP) and expiry against the current time
    private static SignedDataVerifier createVerifier(String bundleId, Long appAppleId, Environment environment) {
        return new SignedDataVerifier(Set.of(rootCertificate()), bundleId, appAppleId, environment, true);
    }

    // Each verifier reads its own stream when it is created
    private static InputStream rootCertificate() {
        try (InputStream inputStream = new ClassPathResource(ROOT_CERTIFICATE).getInputStream()) {
            return new ByteArrayInputStream(inputStream.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + ROOT_CERTIFICATE, e);
        }
    }

    // A malformed value must not keep the app from starting; production notifications are refused until it is fixed
    private static Long parseAppAppleId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.error("Ignoring malformed iap.apple.app-apple-id: {}", value);
            return null;
        }
    }
}
