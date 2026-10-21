package com.renzzle.backend.domain.payment.service;

import com.renzzle.backend.domain.payment.api.response.AppleTestNotificationResponse;
import com.renzzle.backend.domain.payment.api.response.GetAppleTestNotificationStatusResponse;
import com.renzzle.backend.domain.payment.api.response.GetGoogleVoidedPurchaseForAdminResponse;
import com.renzzle.backend.domain.payment.api.response.GetPurchaseForAdminResponse;
import com.renzzle.backend.domain.payment.dao.InAppPurchaseRepository;
import com.renzzle.backend.domain.payment.domain.InAppPurchase;
import com.renzzle.backend.domain.payment.domain.InAppPurchaseStatus;
import com.renzzle.backend.domain.payment.domain.PaymentPlatform;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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
class PaymentAdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-11T00:00:00Z");

    @Mock
    private InAppPurchaseRepository inAppPurchaseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GoogleVoidedPurchaseService googleVoidedPurchaseService;

    @Mock
    private AppleAppStoreServerClient appStoreServerClient;

    private PaymentAdminService paymentAdminService;

    @BeforeEach
    void setUp() {
        RefundPolicy refundPolicy = new RefundPolicy(Clock.fixed(NOW, ZoneOffset.UTC), 7);
        paymentAdminService = new PaymentAdminService(inAppPurchaseRepository, userRepository, refundPolicy,
                googleVoidedPurchaseService, appStoreServerClient);
    }

    @Test
    void searchPurchasesForAdmin_WhenOrderIdMatches_ThenReturnsItWithRefundability() {
        // given
        UserEntity user = user(1500);
        InAppPurchase purchase = purchase(10L, user, "GPA.1", NOW.minus(Duration.ofDays(2)));
        when(inAppPurchaseRepository.findByTransactionId("GPA.1")).thenReturn(Optional.of(purchase));
        when(userRepository.findByIdIncludingDeleted(1L)).thenReturn(user);

        // when
        List<GetPurchaseForAdminResponse> result = paymentAdminService.searchPurchasesForAdmin(" GPA.1 ");

        // then
        assertThat(result).hasSize(1);
        GetPurchaseForAdminResponse found = result.get(0);
        assertThat(found.purchaseId()).isEqualTo(10L);
        assertThat(found.currentCurrency()).isEqualTo(1500);
        assertThat(found.withdrawalDeadline()).isEqualTo(NOW.plus(Duration.ofDays(5)));
        assertThat(found.withinWithdrawalPeriod()).isTrue();
        assertThat(found.unused()).isTrue();
        assertThat(found.refundable()).isTrue();
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).findByNickname(any());
    }

    @Test
    void searchPurchasesForAdmin_WhenKeywordIsNickname_ThenReturnsThatUsersPurchases() {
        // given
        UserEntity user = user(0);
        when(inAppPurchaseRepository.findByTransactionId("tintin")).thenReturn(Optional.empty());
        when(inAppPurchaseRepository.findFirstByPurchaseToken("tintin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("tintin")).thenReturn(Optional.empty());
        when(userRepository.findByNickname("tintin")).thenReturn(Optional.of(user));
        when(inAppPurchaseRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(
                purchase(11L, user, "GPA.2", NOW.minus(Duration.ofDays(1))),
                purchase(10L, user, "GPA.1", NOW.minus(Duration.ofDays(9)))));
        when(userRepository.findByIdIncludingDeleted(1L)).thenReturn(user);

        // when
        List<GetPurchaseForAdminResponse> result = paymentAdminService.searchPurchasesForAdmin("tintin");

        // then
        assertThat(result).extracting(GetPurchaseForAdminResponse::purchaseId).containsExactly(11L, 10L);
        assertThat(result).extracting(GetPurchaseForAdminResponse::refundable).containsExactly(false, false);
        assertThat(result).extracting(GetPurchaseForAdminResponse::withinWithdrawalPeriod).containsExactly(true, false);
    }

    @Test
    void searchPurchasesForAdmin_WhenKeywordIsBlank_ThenThrowsValidationError() {
        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentAdminService.searchPurchasesForAdmin("  "));

        // then
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verifyNoInteractions(inAppPurchaseRepository, userRepository);
    }

    @Test
    void getGoogleVoidedPurchasesForAdmin_ThenShowsTheMatchAndWhatItTakesNewestFirst() {
        // given
        UserEntity user = user(300);
        GoogleVoidedPurchase older = new GoogleVoidedPurchase("token-1", "GPA.1",
                NOW.minus(Duration.ofDays(3)).toEpochMilli(), 0, 1);
        GoogleVoidedPurchase newer = new GoogleVoidedPurchase("token-9", "GPA.9",
                NOW.minus(Duration.ofDays(1)).toEpochMilli(), 2, 8);
        when(googleVoidedPurchaseService.fetchVoidedPurchases()).thenReturn(List.of(older, newer));
        when(googleVoidedPurchaseService.findPurchase(older))
                .thenReturn(Optional.of(purchase(10L, user, "GPA.1", NOW.minus(Duration.ofDays(4)))));
        when(googleVoidedPurchaseService.findPurchase(newer)).thenReturn(Optional.empty());
        when(googleVoidedPurchaseService.shouldReclaimRewards(older)).thenReturn(true);
        when(googleVoidedPurchaseService.shouldReclaimRewards(newer)).thenReturn(false);
        when(userRepository.findByIdIncludingDeleted(1L)).thenReturn(user);

        // when
        List<GetGoogleVoidedPurchaseForAdminResponse> result = paymentAdminService.getGoogleVoidedPurchasesForAdmin();

        // then
        assertThat(result).extracting(GetGoogleVoidedPurchaseForAdminResponse::orderId)
                .containsExactly("GPA.9", "GPA.1");
        assertThat(result.get(0).matched()).isFalse();
        assertThat(result.get(0).voidedReason()).isEqualTo(8);
        GetGoogleVoidedPurchaseForAdminResponse matched = result.get(1);
        assertThat(matched.matched()).isTrue();
        assertThat(matched.status()).isEqualTo("VERIFIED");
        assertThat(matched.reclaimCurrency()).isEqualTo(300);
        assertThat(matched.grantedCurrency()).isEqualTo(1000);
    }

    @Test
    void getGoogleVoidedPurchasesForAdmin_WhenGoogleFails_ThenThrowsStoreApiFailed() {
        // given
        when(googleVoidedPurchaseService.fetchVoidedPurchases()).thenThrow(new RestClientException("timeout"));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentAdminService.getGoogleVoidedPurchasesForAdmin());

        // then
        assertEquals(ErrorCode.STORE_API_FAILED, exception.getErrorCode());
    }

    @Test
    void requestAppleTestNotificationForAdmin_WhenSandbox_ThenAsksTheSandbox() {
        // given
        when(appStoreServerClient.requestTestNotification("Sandbox")).thenReturn("token-1");

        // when
        AppleTestNotificationResponse response = paymentAdminService.requestAppleTestNotificationForAdmin("sandbox");

        // then
        assertThat(response.environment()).isEqualTo("Sandbox");
        assertThat(response.testNotificationToken()).isEqualTo("token-1");
    }

    @Test
    void getAppleTestNotificationStatusForAdmin_ThenConvertsEachAttempt() {
        // given
        when(appStoreServerClient.getTestNotificationStatus("Production", "token-1"))
                .thenReturn(new AppleTestNotificationStatus(List.of(
                        new AppleTestNotificationStatus.SendAttempt(NOW.toEpochMilli(), "SUCCESS"))));

        // when
        GetAppleTestNotificationStatusResponse response =
                paymentAdminService.getAppleTestNotificationStatusForAdmin("Production", "token-1");

        // then
        assertThat(response.sendAttempts()).containsExactly(
                new GetAppleTestNotificationStatusResponse.SendAttempt(NOW, "SUCCESS"));
    }

    private UserEntity user(int currency) {
        return TestUserEntityBuilder.builder()
                .withId(1L)
                .withNickname("tintin")
                .withStatus(Status.getDefaultStatus())
                .withCurrency(currency)
                .build();
    }

    private InAppPurchase purchase(Long id, UserEntity user, String orderId, Instant createdAt) {
        return InAppPurchase.builder()
                .id(id)
                .user(user)
                .platform(PaymentPlatform.ANDROID)
                .productId("piece_1000")
                .transactionId(orderId)
                .purchaseToken("token-" + id)
                .status(InAppPurchaseStatus.VERIFIED)
                .grantedCurrency(1000)
                .createdAt(createdAt)
                .build();
    }
}
