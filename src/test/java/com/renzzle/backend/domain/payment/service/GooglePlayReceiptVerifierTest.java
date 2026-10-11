package com.renzzle.backend.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GooglePlayReceiptVerifierTest {

    @Mock
    private GooglePlayClient googlePlayClient;

    private GooglePlayReceiptVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new GooglePlayReceiptVerifier(googlePlayClient, new ObjectMapper());
    }

    @Test
    void verify_WhenPurchasedAndNotAcknowledged_ThenRequiresAcknowledgement() {
        // given
        when(googlePlayClient.getProductPurchase("piece_1000", "purchase-token"))
                .thenReturn("{\"purchaseState\":0,\"acknowledgementState\":0,\"orderId\":\"GPA.1234\"}");

        // when
        StoreVerificationResult result = verifier.verify("piece_1000", "purchase-token");

        // then
        assertThat(result.productId()).isEqualTo("piece_1000");
        assertThat(result.transactionId()).isEqualTo("GPA.1234");
        assertThat(result.acknowledgementRequired()).isTrue();
    }

    @Test
    void verify_WhenAlreadyAcknowledged_ThenDoesNotRequireAcknowledgement() {
        // given
        when(googlePlayClient.getProductPurchase("piece_1000", "purchase-token"))
                .thenReturn("{\"purchaseState\":0,\"acknowledgementState\":1,\"orderId\":\"GPA.1234\"}");

        // when
        StoreVerificationResult result = verifier.verify("piece_1000", "purchase-token");

        // then
        assertThat(result.acknowledgementRequired()).isFalse();
    }

    @Test
    void verify_WhenPurchaseIsPending_ThenThrowsCustomException() {
        // given
        when(googlePlayClient.getProductPurchase("piece_1000", "purchase-token"))
                .thenReturn("{\"purchaseState\":2,\"acknowledgementState\":0,\"orderId\":\"GPA.1234\"}");

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> verifier.verify("piece_1000", "purchase-token"));

        // then
        assertEquals(ErrorCode.STORE_VERIFICATION_FAILED, exception.getErrorCode());
    }

    @Test
    void verify_WhenGoogleRejectsRequest_ThenThrowsCustomException() {
        // given
        when(googlePlayClient.getProductPurchase("piece_1000", "purchase-token"))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> verifier.verify("piece_1000", "purchase-token"));

        // then
        assertEquals(ErrorCode.STORE_VERIFICATION_FAILED, exception.getErrorCode());
    }
}
