package com.renzzle.backend.domain.payment.service;

import com.renzzle.backend.domain.payment.dao.InAppPurchaseRepository;
import com.renzzle.backend.domain.payment.domain.InAppPurchase;
import com.renzzle.backend.domain.payment.domain.InAppPurchaseStatus;
import com.renzzle.backend.domain.payment.domain.PaymentPlatform;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    private static final Long PURCHASE_ID = 10L;
    private static final Long USER_ID = 1L;
    private static final Instant REFUNDED_AT = Instant.parse("2026-10-11T00:00:00Z");

    @Mock
    private InAppPurchaseRepository inAppPurchaseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefundService refundService;

    @Test
    void reclaim_WhenBalanceCoversGrant_ThenTakesBackFullAmount() {
        // given
        UserEntity user = user(1500);
        InAppPurchase purchase = purchase(user, "piece_1000", 1000);
        stubLocks(purchase, user);

        // when
        boolean refunded = refundService.reclaim(PURCHASE_ID, REFUNDED_AT, "GOOGLE/1", true);

        // then
        assertThat(refunded).isTrue();
        assertThat(user.getCurrency()).isEqualTo(500);
        assertThat(purchase.getStatus()).isEqualTo(InAppPurchaseStatus.REFUNDED);
        assertThat(purchase.getReclaimedCurrency()).isEqualTo(1000);
        assertThat(purchase.getRefundedAt()).isEqualTo(REFUNDED_AT);
        assertThat(purchase.getRefundReason()).isEqualTo("GOOGLE/1");
    }

    @Test
    void reclaim_WhenUserSpentPartOfIt_ThenStopsAtZero() {
        // given
        UserEntity user = user(300);
        InAppPurchase purchase = purchase(user, "piece_1000", 1000);
        stubLocks(purchase, user);

        // when
        refundService.reclaim(PURCHASE_ID, REFUNDED_AT, "APPLE/0", true);

        // then
        assertThat(user.getCurrency()).isZero();
        assertThat(purchase.getReclaimedCurrency()).isEqualTo(300);
        assertThat(purchase.getStatus()).isEqualTo(InAppPurchaseStatus.REFUNDED);
    }

    @Test
    void reclaim_WhenAlreadyRefunded_ThenChangesNothing() {
        // given
        UserEntity user = user(1000);
        InAppPurchase purchase = purchase(user, "piece_1000", 1000);
        purchase.markRefunded(REFUNDED_AT, "GOOGLE/1", 1000);
        when(inAppPurchaseRepository.findByIdForUpdate(PURCHASE_ID)).thenReturn(Optional.of(purchase));

        // when
        boolean refunded = refundService.reclaim(PURCHASE_ID, REFUNDED_AT, "GOOGLE/1", true);

        // then
        assertThat(refunded).isFalse();
        assertThat(user.getCurrency()).isEqualTo(1000);
        verifyNoInteractions(userRepository);
    }

    @Test
    void reclaim_WhenRemoveAdsHasNoOtherPurchase_ThenShowsAdsAgain() {
        // given
        UserEntity user = user(0);
        user.removeAds();
        InAppPurchase purchase = purchase(user, "remove_ads", 0);
        stubLocks(purchase, user);
        when(inAppPurchaseRepository.existsByUserIdAndProductIdAndStatusAndIdNot(
                USER_ID, "remove_ads", InAppPurchaseStatus.VERIFIED, PURCHASE_ID)).thenReturn(false);

        // when
        refundService.reclaim(PURCHASE_ID, REFUNDED_AT, "APPLE/0", true);

        // then
        assertThat(user.isAdsRemoved()).isFalse();
        assertThat(purchase.getStatus()).isEqualTo(InAppPurchaseStatus.REFUNDED);
    }

    @Test
    void reclaim_WhenRemoveAdsBoughtOnOtherPlatform_ThenKeepsAdsRemoved() {
        // given
        UserEntity user = user(0);
        user.removeAds();
        InAppPurchase purchase = purchase(user, "remove_ads", 0);
        stubLocks(purchase, user);
        when(inAppPurchaseRepository.existsByUserIdAndProductIdAndStatusAndIdNot(
                USER_ID, "remove_ads", InAppPurchaseStatus.VERIFIED, PURCHASE_ID)).thenReturn(true);

        // when
        refundService.reclaim(PURCHASE_ID, REFUNDED_AT, "APPLE/0", true);

        // then
        assertThat(user.isAdsRemoved()).isTrue();
        assertThat(purchase.getStatus()).isEqualTo(InAppPurchaseStatus.REFUNDED);
    }

    @Test
    void reclaim_WhenRewardsNotReclaimed_ThenOnlyRecordsRefund() {
        // given
        UserEntity user = user(1000);
        InAppPurchase purchase = purchase(user, "piece_1000", 1000);
        stubLocks(purchase, user);

        // when
        boolean refunded = refundService.reclaim(PURCHASE_ID, REFUNDED_AT, "GOOGLE/8", false);

        // then
        assertThat(refunded).isTrue();
        assertThat(user.getCurrency()).isEqualTo(1000);
        assertThat(purchase.getStatus()).isEqualTo(InAppPurchaseStatus.REFUNDED);
        assertThat(purchase.getReclaimedCurrency()).isZero();
    }

    @Test
    void reclaim_WhenUserIsDeleted_ThenOnlyRecordsRefund() {
        // given
        UserEntity user = user(1000);
        InAppPurchase purchase = purchase(user, "piece_1000", 1000);
        when(inAppPurchaseRepository.findByIdForUpdate(PURCHASE_ID)).thenReturn(Optional.of(purchase));
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        // when
        boolean refunded = refundService.reclaim(PURCHASE_ID, REFUNDED_AT, "GOOGLE/1", true);

        // then
        assertThat(refunded).isTrue();
        assertThat(purchase.getStatus()).isEqualTo(InAppPurchaseStatus.REFUNDED);
        assertThat(purchase.getReclaimedCurrency()).isZero();
    }

    @Test
    void cancelRefund_WhenRefunded_ThenReturnsOnlyWhatWasReclaimed() {
        // given
        UserEntity user = user(0);
        InAppPurchase purchase = purchase(user, "piece_1000", 1000);
        purchase.markRefunded(REFUNDED_AT, "APPLE/0", 300);
        stubLocks(purchase, user);

        // when
        boolean reversed = refundService.cancelRefund(PURCHASE_ID);

        // then
        assertThat(reversed).isTrue();
        assertThat(user.getCurrency()).isEqualTo(300);
        assertThat(purchase.getStatus()).isEqualTo(InAppPurchaseStatus.VERIFIED);
        assertThat(purchase.getReclaimedCurrency()).isZero();
        assertThat(purchase.getRefundedAt()).isNull();
        assertThat(purchase.getRefundReason()).isNull();
    }

    @Test
    void cancelRefund_WhenRemoveAdsRefunded_ThenRemovesAdsAgain() {
        // given
        UserEntity user = user(0);
        InAppPurchase purchase = purchase(user, "remove_ads", 0);
        purchase.markRefunded(REFUNDED_AT, "APPLE/0", 0);
        stubLocks(purchase, user);

        // when
        refundService.cancelRefund(PURCHASE_ID);

        // then
        assertThat(user.isAdsRemoved()).isTrue();
        assertThat(purchase.getStatus()).isEqualTo(InAppPurchaseStatus.VERIFIED);
    }

    @Test
    void cancelRefund_WhenNotRefunded_ThenChangesNothing() {
        // given
        UserEntity user = user(0);
        InAppPurchase purchase = purchase(user, "piece_1000", 1000);
        when(inAppPurchaseRepository.findByIdForUpdate(PURCHASE_ID)).thenReturn(Optional.of(purchase));

        // when
        boolean reversed = refundService.cancelRefund(PURCHASE_ID);

        // then
        assertThat(reversed).isFalse();
        assertThat(user.getCurrency()).isZero();
        verifyNoInteractions(userRepository);
    }

    private UserEntity user(int currency) {
        return TestUserEntityBuilder.builder().withId(USER_ID).withCurrency(currency).build();
    }

    private InAppPurchase purchase(UserEntity user, String productId, int grantedCurrency) {
        return InAppPurchase.builder()
                .id(PURCHASE_ID)
                .user(user)
                .platform(PaymentPlatform.ANDROID)
                .productId(productId)
                .transactionId("GPA.1234")
                .purchaseToken("purchase-token")
                .status(InAppPurchaseStatus.VERIFIED)
                .grantedCurrency(grantedCurrency)
                .build();
    }

    private void stubLocks(InAppPurchase purchase, UserEntity user) {
        when(inAppPurchaseRepository.findByIdForUpdate(PURCHASE_ID)).thenReturn(Optional.of(purchase));
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    }
}
