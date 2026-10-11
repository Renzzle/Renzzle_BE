package com.renzzle.backend.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

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
    void verify_WhenPurchaseIsPending_ThenThrowsPurchasePending() {
        // given
        when(googlePlayClient.getProductPurchase("piece_1000", "purchase-token"))
                .thenReturn("{\"purchaseState\":2,\"acknowledgementState\":0,\"orderId\":\"GPA.1234\"}");

        // when, then
        assertErrorCode(ErrorCode.PURCHASE_PENDING);
    }

    @Test
    void verify_WhenPurchaseIsCanceled_ThenThrowsStoreVerificationFailed() {
        // given
        when(googlePlayClient.getProductPurchase("piece_1000", "purchase-token"))
                .thenReturn("{\"purchaseState\":1,\"acknowledgementState\":0,\"orderId\":\"GPA.1234\"}");

        // when, then
        assertErrorCode(ErrorCode.STORE_VERIFICATION_FAILED);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 410})
    void verify_WhenGoogleRejectsTheToken_ThenThrowsStoreVerificationFailed(int status) {
        // given
        when(googlePlayClient.getProductPurchase("piece_1000", "purchase-token"))
                .thenThrow(statusError(status));

        // when, then
        assertErrorCode(ErrorCode.STORE_VERIFICATION_FAILED);
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404, 429, 500, 503})
    void verify_WhenTheCallFails_ThenThrowsStoreApiFailedSoTheAppRetries(int status) {
        // given
        when(googlePlayClient.getProductPurchase("piece_1000", "purchase-token"))
                .thenThrow(statusError(status));

        // when, then
        assertErrorCode(ErrorCode.STORE_API_FAILED);
    }

    @Test
    void verify_WhenGoogleIsUnreachable_ThenThrowsStoreApiFailed() {
        // given
        when(googlePlayClient.getProductPurchase("piece_1000", "purchase-token"))
                .thenThrow(new ResourceAccessException("Read timed out"));

        // when, then
        assertErrorCode(ErrorCode.STORE_API_FAILED);
    }

    @Test
    void verify_WhenResponseCannotBeRead_ThenThrowsStoreApiFailed() {
        // given
        when(googlePlayClient.getProductPurchase("piece_1000", "purchase-token")).thenReturn("<html>");

        // when, then
        assertErrorCode(ErrorCode.STORE_API_FAILED);
    }

    private void assertErrorCode(ErrorCode expected) {
        CustomException exception = assertThrows(CustomException.class,
                () -> verifier.verify("piece_1000", "purchase-token"));
        assertEquals(expected, exception.getErrorCode());
    }

    private HttpStatusCodeException statusError(int status) {
        HttpStatus httpStatus = HttpStatus.valueOf(status);
        return httpStatus.is4xxClientError()
                ? HttpClientErrorException.create(httpStatus, httpStatus.getReasonPhrase(), null, null, null)
                : HttpServerErrorException.create(httpStatus, httpStatus.getReasonPhrase(), null, null, null);
    }
}
