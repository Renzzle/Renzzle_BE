package com.renzzle.backend.domain.payment.service;

import com.renzzle.backend.domain.payment.domain.IapProduct;
import com.renzzle.backend.domain.payment.domain.InAppPurchase;
import com.renzzle.backend.domain.payment.domain.InAppPurchaseStatus;
import com.renzzle.backend.domain.user.domain.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

// When we would agree to a refund: within the withdrawal period and with the granted currency unused.
// CS follows it for Google refunds and the App Store receives it as our preference
@Component
public class RefundPolicy {

    // App Store consumption percentages are in thousandths of a percent
    static final int FULLY_CONSUMED = 100_000;

    private final Clock clock;
    private final int withdrawalDays;

    public RefundPolicy(Clock clock, @Value("${iap.refund.withdrawal-days}") int withdrawalDays) {
        this.clock = clock;
        this.withdrawalDays = withdrawalDays;
    }

    // Counted from server verification, which follows the payment within moments
    public Instant withdrawalDeadline(InAppPurchase purchase) {
        return purchase.getCreatedAt().plus(withdrawalDays, ChronoUnit.DAYS);
    }

    public boolean isWithinWithdrawalPeriod(InAppPurchase purchase) {
        return clock.instant().isBefore(withdrawalDeadline(purchase));
    }

    // Purchased currency mixes with puzzle rewards and spending is not recorded, so a balance at least as large
    // as the grant counts as unused. remove_ads has nothing to use up
    public boolean isUnused(InAppPurchase purchase, UserEntity user) {
        return !grantsCurrency(purchase) || user.getCurrency() >= purchase.getGrantedCurrency();
    }

    public boolean isRefundable(InAppPurchase purchase, UserEntity user) {
        return purchase.getStatus() == InAppPurchaseStatus.VERIFIED
                && isWithinWithdrawalPeriod(purchase)
                && isUnused(purchase, user);
    }

    // remove_ads takes effect the moment it is bought, so it counts as fully consumed
    public int consumptionPercentage(InAppPurchase purchase, UserEntity user) {
        if (!grantsCurrency(purchase)) {
            return FULLY_CONSUMED;
        }
        int granted = purchase.getGrantedCurrency();
        if (granted <= 0 || isUnused(purchase, user)) {
            return 0;
        }
        int spent = granted - Math.max(user.getCurrency(), 0);
        return (int) Math.round((double) spent * FULLY_CONSUMED / granted);
    }

    private boolean grantsCurrency(InAppPurchase purchase) {
        return IapProduct.fromProductId(purchase.getProductId()).grantsCurrency();
    }
}
