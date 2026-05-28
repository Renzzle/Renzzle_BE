package com.renzzle.backend.domain.payment.service;

import com.renzzle.backend.domain.payment.api.request.VerifyInAppPurchaseRequest;
import com.renzzle.backend.domain.payment.api.response.VerifyInAppPurchaseResponse;
import com.renzzle.backend.domain.payment.dao.InAppPurchaseRepository;
import com.renzzle.backend.domain.payment.domain.InAppPurchase;
import com.renzzle.backend.domain.payment.domain.PaymentPlatform;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private InAppPurchaseRepository inAppPurchaseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GooglePlayReceiptVerifier googlePlayReceiptVerifier;

    @Mock
    private AppleReceiptVerifier appleReceiptVerifier;

    @InjectMocks
    private PaymentService paymentService;

    @DisplayName("iOS 검증 요청에서 transactionId가 없으면 실패")
    @Test
    void verifyInAppPurchase_WhenIosTransactionIdMissing_ThenThrowsCustomException() {
        // given
        UserEntity user = TestUserEntityBuilder.builder().withId(1L).build();
        VerifyInAppPurchaseRequest request =
                new VerifyInAppPurchaseRequest("ios", "coin_100", null, null, "receipt");

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.verifyInAppPurchase(user, request));

        // then
        assertEquals(ErrorCode.INVALID_PAYMENT_REQUEST, exception.getErrorCode());
        verifyNoInteractions(appleReceiptVerifier);
    }

    @DisplayName("iOS 검증 요청에서 receipt가 없으면 실패")
    @Test
    void verifyInAppPurchase_WhenIosReceiptMissing_ThenThrowsCustomException() {
        // given
        UserEntity user = TestUserEntityBuilder.builder().withId(1L).build();
        VerifyInAppPurchaseRequest request =
                new VerifyInAppPurchaseRequest("ios", "coin_100", "transaction-id", null, null);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.verifyInAppPurchase(user, request));

        // then
        assertEquals(ErrorCode.INVALID_PAYMENT_REQUEST, exception.getErrorCode());
        verifyNoInteractions(appleReceiptVerifier);
    }

    @DisplayName("iOS 검증 응답의 transactionId가 요청과 다르면 실패")
    @Test
    void verifyInAppPurchase_WhenIosTransactionMismatch_ThenThrowsCustomException() {
        // given
        UserEntity user = TestUserEntityBuilder.builder().withId(1L).build();
        VerifyInAppPurchaseRequest request =
                new VerifyInAppPurchaseRequest("ios", "coin_100", "transaction-id", null, "receipt");

        when(inAppPurchaseRepository.existsByTransactionId("transaction-id")).thenReturn(false);
        when(appleReceiptVerifier.verify("coin_100", "transaction-id", "receipt"))
                .thenReturn(new StoreVerificationResult("coin_100", "other-transaction-id"));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.verifyInAppPurchase(user, request));

        // then
        assertEquals(ErrorCode.RECEIPT_TRANSACTION_MISMATCH, exception.getErrorCode());
        verify(inAppPurchaseRepository, never()).save(any(InAppPurchase.class));
    }

    @DisplayName("Android 검증 요청에서 purchaseToken이 없으면 실패")
    @Test
    void verifyInAppPurchase_WhenAndroidPurchaseTokenMissing_ThenThrowsCustomException() {
        // given
        UserEntity user = TestUserEntityBuilder.builder().withId(1L).build();
        VerifyInAppPurchaseRequest request =
                new VerifyInAppPurchaseRequest("android", "coin_100", "order-id", null, null);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.verifyInAppPurchase(user, request));

        // then
        assertEquals(ErrorCode.INVALID_PAYMENT_REQUEST, exception.getErrorCode());
        verifyNoInteractions(googlePlayReceiptVerifier);
    }

    @DisplayName("중복 purchaseToken은 지급하지 않고 실패")
    @Test
    void verifyInAppPurchase_WhenPurchaseTokenDuplicated_ThenThrowsCustomException() {
        // given
        UserEntity user = TestUserEntityBuilder.builder().withId(1L).build();
        VerifyInAppPurchaseRequest request =
                new VerifyInAppPurchaseRequest("android", "coin_100", null, "purchase-token", null);

        when(inAppPurchaseRepository.existsByPurchaseToken("purchase-token")).thenReturn(true);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.verifyInAppPurchase(user, request));

        // then
        assertEquals(ErrorCode.ALREADY_PROCESSED_RECEIPT, exception.getErrorCode());
        verifyNoInteractions(googlePlayReceiptVerifier);
        verify(inAppPurchaseRepository, never()).save(any(InAppPurchase.class));
    }

    @DisplayName("중복 transactionId는 지급하지 않고 실패")
    @Test
    void verifyInAppPurchase_WhenTransactionIdDuplicated_ThenThrowsCustomException() {
        // given
        UserEntity user = TestUserEntityBuilder.builder().withId(1L).build();
        VerifyInAppPurchaseRequest request =
                new VerifyInAppPurchaseRequest("ios", "coin_100", "transaction-id", null, "receipt");

        when(inAppPurchaseRepository.existsByTransactionId("transaction-id")).thenReturn(true);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.verifyInAppPurchase(user, request));

        // then
        assertEquals(ErrorCode.ALREADY_PROCESSED_RECEIPT, exception.getErrorCode());
        verifyNoInteractions(appleReceiptVerifier);
        verify(inAppPurchaseRepository, never()).save(any(InAppPurchase.class));
    }

    @DisplayName("정상 Android 결제는 결제 기록 저장 후 재화를 지급")
    @Test
    void verifyInAppPurchase_WhenAndroidPurchaseValid_ThenSavePurchaseAndGrantReward() {
        // given
        UserEntity principalUser = TestUserEntityBuilder.builder().withId(1L).build();
        UserEntity persistedUser = TestUserEntityBuilder.builder().withId(1L).withCurrency(0).build();
        VerifyInAppPurchaseRequest request =
                new VerifyInAppPurchaseRequest("android", "coin_100", null, "purchase-token", null);

        when(inAppPurchaseRepository.existsByPurchaseToken("purchase-token")).thenReturn(false);
        when(googlePlayReceiptVerifier.verify("coin_100", "purchase-token"))
                .thenReturn(new StoreVerificationResult("coin_100", "order-id"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(persistedUser));

        // when
        VerifyInAppPurchaseResponse response = paymentService.verifyInAppPurchase(principalUser, request);

        // then
        ArgumentCaptor<InAppPurchase> captor = ArgumentCaptor.forClass(InAppPurchase.class);
        verify(inAppPurchaseRepository).save(captor.capture());

        InAppPurchase savedPurchase = captor.getValue();
        assertThat(savedPurchase.getPlatform()).isEqualTo(PaymentPlatform.ANDROID);
        assertThat(savedPurchase.getProductId()).isEqualTo("coin_100");
        assertThat(savedPurchase.getPurchaseToken()).isEqualTo("purchase-token");
        assertThat(savedPurchase.getTransactionId()).isEqualTo("order-id");
        assertThat(savedPurchase.getGrantedCurrency()).isEqualTo(100);
        assertThat(persistedUser.getCurrency()).isEqualTo(100);

        assertThat(response.platform()).isEqualTo("ANDROID");
        assertThat(response.productId()).isEqualTo("coin_100");
        assertThat(response.grantedCurrency()).isEqualTo(100);
    }
}
