package com.renzzle.backend.domain.payment.service;

import com.apple.itunes.storekit.verification.SignedDataVerifier;
import com.apple.itunes.storekit.verification.VerificationException;
import com.apple.itunes.storekit.verification.VerificationStatus;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppleNotificationVerifierTest {

    private static final String SIGNED_PAYLOAD = "header.payload.signature";

    @Mock
    private SignedDataVerifier productionVerifier;

    @Mock
    private SignedDataVerifier sandboxVerifier;

    @Test
    void verify_WhenProduction_ThenUsesTheProductionVerifier() throws Exception {
        // given
        AppleNotificationVerifier verifier = new AppleNotificationVerifier(productionVerifier, sandboxVerifier);

        // when
        verifier.verify(SIGNED_PAYLOAD, "Production");

        // then
        verify(productionVerifier).verifyAndDecodeNotification(SIGNED_PAYLOAD);
        verifyNoInteractions(sandboxVerifier);
    }

    @Test
    void verify_WhenSandbox_ThenUsesTheSandboxVerifier() throws Exception {
        // given
        AppleNotificationVerifier verifier = new AppleNotificationVerifier(productionVerifier, sandboxVerifier);

        // when
        verifier.verify(SIGNED_PAYLOAD, "Sandbox");

        // then
        verify(sandboxVerifier).verifyAndDecodeNotification(SIGNED_PAYLOAD);
        verifyNoInteractions(productionVerifier);
    }

    @ParameterizedTest
    @EnumSource(value = VerificationStatus.class, names = {
            "VERIFICATION_FAILURE", "INVALID_APP_IDENTIFIER", "INVALID_ENVIRONMENT",
            "INVALID_CERTIFICATE", "INVALID_CHAIN_LENGTH", "INVALID_CHAIN"})
    void verify_WhenAppleDidNotSignIt_ThenThrowsInvalidStoreNotification(VerificationStatus status) throws Exception {
        // given
        AppleNotificationVerifier verifier = new AppleNotificationVerifier(productionVerifier, sandboxVerifier);
        when(productionVerifier.verifyAndDecodeNotification(SIGNED_PAYLOAD)).thenThrow(new VerificationException(status));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> verifier.verify(SIGNED_PAYLOAD, "Production"));

        // then
        assertEquals(ErrorCode.INVALID_STORE_NOTIFICATION, exception.getErrorCode());
    }

    @Test
    void verify_WhenRevocationCannotBeCheckedNow_ThenThrowsStoreApiFailedSoAppleRetries() throws Exception {
        // given
        AppleNotificationVerifier verifier = new AppleNotificationVerifier(productionVerifier, sandboxVerifier);
        when(productionVerifier.verifyAndDecodeNotification(SIGNED_PAYLOAD))
                .thenThrow(new VerificationException(VerificationStatus.RETRYABLE_VERIFICATION_FAILURE));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> verifier.verify(SIGNED_PAYLOAD, "Production"));

        // then
        assertEquals(ErrorCode.STORE_API_FAILED, exception.getErrorCode());
    }

    @Test
    void verify_WhenAppAppleIdIsNotConfigured_ThenRefusesProductionButStillVerifiesSandbox() throws Exception {
        // given
        AppleNotificationVerifier verifier = new AppleNotificationVerifier(null, sandboxVerifier);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> verifier.verify(SIGNED_PAYLOAD, "Production"));
        verifier.verify(SIGNED_PAYLOAD, "Sandbox");

        // then
        assertEquals(ErrorCode.STORE_API_FAILED, exception.getErrorCode());
        verify(sandboxVerifier).verifyAndDecodeNotification(SIGNED_PAYLOAD);
    }

    @Test
    void constructor_WithTheBundledRootCertificate_ThenRejectsASelfSignedPayload() {
        // given: built the way the application builds it, from the bundled Apple Root CA - G3
        AppleNotificationVerifier verifier = new AppleNotificationVerifier("com.renzzle", "1234567890");

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> verifier.verify(fakeJws(), "Sandbox"));

        // then
        assertEquals(ErrorCode.INVALID_STORE_NOTIFICATION, exception.getErrorCode());
    }

    @Test
    void constructor_WhenAppAppleIdIsMalformed_ThenStillStarts() {
        assertDoesNotThrow(() -> new AppleNotificationVerifier("com.renzzle", "not-a-number"));
    }

    // A JWS with no x5c certificate chain, as anyone without Apple's key could make
    private String fakeJws() {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(("{\"notificationType\":\"REFUND\",\"data\":"
                + "{\"bundleId\":\"com.renzzle\",\"environment\":\"Sandbox\"}}").getBytes(StandardCharsets.UTF_8));
        String signature = encoder.encodeToString("fake-signature".getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + "." + signature;
    }
}
