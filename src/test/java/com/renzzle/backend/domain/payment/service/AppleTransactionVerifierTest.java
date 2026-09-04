package com.renzzle.backend.domain.payment.service;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppleTransactionVerifierTest {

    private static final String BUNDLE_ID = "com.renzzle";
    private static final String TRANSACTION_ID = "2000000123456789";

    @Mock
    private AppleAppStoreServerClient appStoreServerClient;

    private AppleTransactionVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new AppleTransactionVerifier(appStoreServerClient, BUNDLE_ID);
    }

    @Test
    void verify_WhenTransactionMatches_ThenReturnsVerificationResult() {
        // given
        when(appStoreServerClient.fetchTransactionInfo(TRANSACTION_ID))
                .thenReturn(transaction(TRANSACTION_ID, "piece_1000", BUNDLE_ID, null));

        // when
        StoreVerificationResult result = verifier.verify("piece_1000", TRANSACTION_ID);

        // then
        assertThat(result.productId()).isEqualTo("piece_1000");
        assertThat(result.transactionId()).isEqualTo(TRANSACTION_ID);
    }

    @Test
    void verify_WhenBundleIdMismatch_ThenThrowsCustomException() {
        // given
        when(appStoreServerClient.fetchTransactionInfo(TRANSACTION_ID))
                .thenReturn(transaction(TRANSACTION_ID, "piece_1000", "com.other.app", null));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> verifier.verify("piece_1000", TRANSACTION_ID));

        // then
        assertEquals(ErrorCode.STORE_VERIFICATION_FAILED, exception.getErrorCode());
    }

    @Test
    void verify_WhenTransactionRevoked_ThenThrowsCustomException() {
        // given
        when(appStoreServerClient.fetchTransactionInfo(TRANSACTION_ID))
                .thenReturn(transaction(TRANSACTION_ID, "piece_1000", BUNDLE_ID, 1720000000000L));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> verifier.verify("piece_1000", TRANSACTION_ID));

        // then
        assertEquals(ErrorCode.STORE_VERIFICATION_FAILED, exception.getErrorCode());
    }

    @Test
    void verify_WhenProductIdMismatch_ThenThrowsCustomException() {
        // given
        when(appStoreServerClient.fetchTransactionInfo(TRANSACTION_ID))
                .thenReturn(transaction(TRANSACTION_ID, "piece_10000", BUNDLE_ID, null));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> verifier.verify("piece_1000", TRANSACTION_ID));

        // then
        assertEquals(ErrorCode.RECEIPT_TRANSACTION_MISMATCH, exception.getErrorCode());
    }

    private static AppleTransactionInfo transaction(
            String transactionId,
            String productId,
            String bundleId,
            Long revocationDate
    ) {
        return new AppleTransactionInfo(
                transactionId,
                transactionId,
                bundleId,
                productId,
                "Consumable",
                "Sandbox",
                1720000000000L,
                1,
                "PURCHASED",
                "PURCHASE",
                revocationDate,
                revocationDate == null ? null : 0
        );
    }
}
