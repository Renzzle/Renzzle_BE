package com.renzzle.backend.domain.payment.service;

import com.renzzle.backend.domain.payment.domain.InAppPurchase;
import com.renzzle.backend.domain.payment.domain.InAppPurchaseStatus;
import com.renzzle.backend.domain.payment.domain.PaymentPlatform;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RefundPolicyTest {

    private static final Instant NOW = Instant.parse("2026-10-11T00:00:00Z");
    private static final Duration WITHDRAWAL_PERIOD = Duration.ofDays(7);

    private final RefundPolicy refundPolicy = new RefundPolicy(Clock.fixed(NOW, ZoneOffset.UTC), 7);

    @Test
    void isWithinWithdrawalPeriod_WhenJustBeforeDeadline_ThenTrue() {
        // given
        InAppPurchase purchase = purchase("piece_1000", 1000, NOW.minus(WITHDRAWAL_PERIOD).plusSeconds(1));

        // when, then
        assertThat(refundPolicy.isWithinWithdrawalPeriod(purchase)).isTrue();
        assertThat(refundPolicy.withdrawalDeadline(purchase)).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void isWithinWithdrawalPeriod_WhenDeadlineReached_ThenFalse() {
        // given
        InAppPurchase purchase = purchase("piece_1000", 1000, NOW.minus(WITHDRAWAL_PERIOD));

        // when, then
        assertThat(refundPolicy.isWithinWithdrawalPeriod(purchase)).isFalse();
    }

    @Test
    void isRefundable_WhenWithinPeriodAndBalanceCoversGrant_ThenTrue() {
        // given
        InAppPurchase purchase = purchase("piece_1000", 1000, NOW.minus(Duration.ofDays(1)));

        // when, then
        assertThat(refundPolicy.isRefundable(purchase, user(1000))).isTrue();
    }

    @Test
    void isRefundable_WhenBalanceBelowGrant_ThenFalse() {
        // given
        InAppPurchase purchase = purchase("piece_1000", 1000, NOW.minus(Duration.ofDays(1)));

        // when, then
        assertThat(refundPolicy.isUnused(purchase, user(999))).isFalse();
        assertThat(refundPolicy.isRefundable(purchase, user(999))).isFalse();
    }

    @Test
    void isRefundable_WhenPeriodPassed_ThenFalse() {
        // given
        InAppPurchase purchase = purchase("piece_1000", 1000, NOW.minus(Duration.ofDays(8)));

        // when, then
        assertThat(refundPolicy.isRefundable(purchase, user(5000))).isFalse();
    }

    @Test
    void isRefundable_WhenAlreadyRefunded_ThenFalse() {
        // given
        InAppPurchase purchase = purchase("piece_1000", 1000, NOW.minus(Duration.ofDays(1)));
        purchase.markRefunded(NOW, "GOOGLE/1", 1000);

        // when, then
        assertThat(refundPolicy.isRefundable(purchase, user(5000))).isFalse();
    }

    @Test
    void isRefundable_WhenRemoveAdsWithinPeriod_ThenTrueWhateverTheBalance() {
        // given
        InAppPurchase purchase = purchase("remove_ads", 0, NOW.minus(Duration.ofDays(1)));

        // when, then
        assertThat(refundPolicy.isRefundable(purchase, user(0))).isTrue();
    }

    @Test
    void consumptionPercentage_WhenBalanceCoversGrant_ThenZero() {
        // given
        InAppPurchase purchase = purchase("piece_1000", 1000, NOW);

        // when, then
        assertThat(refundPolicy.consumptionPercentage(purchase, user(1200))).isZero();
    }

    @Test
    void consumptionPercentage_WhenPartlySpent_ThenSpentShareInThousandthsOfAPercent() {
        // given
        InAppPurchase purchase = purchase("piece_1000", 1000, NOW);

        // when, then
        assertThat(refundPolicy.consumptionPercentage(purchase, user(250))).isEqualTo(75_000);
    }

    @Test
    void consumptionPercentage_WhenAllSpent_ThenFullyConsumed() {
        // given
        InAppPurchase purchase = purchase("piece_1000", 1000, NOW);

        // when, then
        assertThat(refundPolicy.consumptionPercentage(purchase, user(0))).isEqualTo(RefundPolicy.FULLY_CONSUMED);
    }

    @Test
    void consumptionPercentage_WhenRemoveAds_ThenFullyConsumed() {
        // given
        InAppPurchase purchase = purchase("remove_ads", 0, NOW);

        // when, then
        assertThat(refundPolicy.consumptionPercentage(purchase, user(5000))).isEqualTo(RefundPolicy.FULLY_CONSUMED);
    }

    private UserEntity user(int currency) {
        return TestUserEntityBuilder.builder().withId(1L).withCurrency(currency).build();
    }

    private InAppPurchase purchase(String productId, int grantedCurrency, Instant createdAt) {
        return InAppPurchase.builder()
                .id(10L)
                .user(user(0))
                .platform(PaymentPlatform.IOS)
                .productId(productId)
                .transactionId("2000000123456789")
                .status(InAppPurchaseStatus.VERIFIED)
                .grantedCurrency(grantedCurrency)
                .createdAt(createdAt)
                .build();
    }
}
