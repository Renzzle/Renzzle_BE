package com.renzzle.backend.domain.payment.service;

import com.renzzle.backend.domain.payment.dao.InAppPurchaseRepository;
import com.renzzle.backend.domain.payment.domain.IapProduct;
import com.renzzle.backend.domain.payment.domain.InAppPurchase;
import com.renzzle.backend.domain.payment.domain.InAppPurchaseStatus;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

// Applies refunds the stores already made. The server never issues a refund itself
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final InAppPurchaseRepository inAppPurchaseRepository;
    private final UserRepository userRepository;

    // Takes back what the purchase granted, never below zero. With reclaimRewards false it only records the refund.
    // Returns false when the purchase was not VERIFIED, so a repeated notification changes nothing
    @Transactional
    public boolean reclaim(Long purchaseId, Instant refundedAt, String refundReason, boolean reclaimRewards) {
        InAppPurchase purchase = findPurchaseForUpdate(purchaseId);
        if (purchase.getStatus() != InAppPurchaseStatus.VERIFIED) {
            return false;
        }

        int reclaimedCurrency = 0;
        Optional<UserEntity> user = findUserForUpdate(purchase);
        if (reclaimRewards && user.isPresent()) {
            IapProduct product = IapProduct.fromProductId(purchase.getProductId());
            if (product.grantsCurrency()) {
                reclaimedCurrency = user.get().reclaimCurrency(purchase.getGrantedCurrency());
            }
            if (product.removesAds() && !hasOtherRemoveAdsPurchase(purchase)) {
                user.get().restoreAds();
            }
        }

        purchase.markRefunded(refundedAt, refundReason, reclaimedCurrency);
        log.info("In-app purchase refunded. purchaseId={}, productId={}, reason={}, grantedCurrency={}, "
                        + "reclaimedCurrency={}, reclaimRewards={}",
                purchase.getId(), purchase.getProductId(), refundReason, purchase.getGrantedCurrency(),
                reclaimedCurrency, reclaimRewards);
        return true;
    }

    // The store reversed a refund: gives back only what the refund took, not what was written off
    @Transactional
    public boolean cancelRefund(Long purchaseId) {
        InAppPurchase purchase = findPurchaseForUpdate(purchaseId);
        if (purchase.getStatus() != InAppPurchaseStatus.REFUNDED) {
            return false;
        }

        int returnedCurrency = purchase.getReclaimedCurrency();
        findUserForUpdate(purchase).ifPresent(user -> {
            user.getReward(returnedCurrency);
            if (IapProduct.fromProductId(purchase.getProductId()).removesAds()) {
                user.removeAds();
            }
        });

        purchase.cancelRefund();
        log.info("In-app purchase refund reversed. purchaseId={}, productId={}, returnedCurrency={}",
                purchase.getId(), purchase.getProductId(), returnedCurrency);
        return true;
    }

    // Locks the purchase before the user, the same order for every refund path, so they can't deadlock
    private InAppPurchase findPurchaseForUpdate(Long purchaseId) {
        return inAppPurchaseRepository.findByIdForUpdate(purchaseId)
                .orElseThrow(() -> new CustomException(ErrorCode.GLOBAL_NOT_FOUND));
    }

    // Empty for a deleted user, whose refund is then only recorded
    private Optional<UserEntity> findUserForUpdate(InAppPurchase purchase) {
        return userRepository.findByIdForUpdate(purchase.getUser().getId());
    }

    // remove_ads bought on the other platform keeps the ads off
    private boolean hasOtherRemoveAdsPurchase(InAppPurchase purchase) {
        return inAppPurchaseRepository.existsByUserIdAndProductIdAndStatusAndIdNot(
                purchase.getUser().getId(), purchase.getProductId(), InAppPurchaseStatus.VERIFIED, purchase.getId());
    }
}
