package com.renzzle.backend.domain.payment.dao;

import com.renzzle.backend.domain.payment.domain.InAppPurchase;
import com.renzzle.backend.domain.payment.domain.InAppPurchaseStatus;
import com.renzzle.backend.domain.payment.domain.PaymentPlatform;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.support.DataJpaTestWithInitContainers;
import com.renzzle.backend.support.TestUserEntityBuilder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithInitContainers
class InAppPurchaseRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InAppPurchaseRepository inAppPurchaseRepository;

    @Test
    void statusColumn_IsVarchar_SoAddingAStatusNeedsNoColumnChange() {
        // when
        String dataType = jdbcTemplate.queryForObject(
                "SELECT DATA_TYPE FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'in_app_purchase' AND COLUMN_NAME = 'status'",
                String.class);

        // then
        assertThat(dataType).isEqualToIgnoringCase("varchar");
    }

    @Test
    void markRefunded_ThenStoresRefundedStatusAndDetails() {
        // given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        InAppPurchase purchase = inAppPurchaseRepository.save(
                purchase(user, PaymentPlatform.ANDROID, "piece_1000", "GPA.1", "token-1"));
        Instant refundedAt = Instant.parse("2026-10-11T00:00:00Z");

        // when
        purchase.markRefunded(refundedAt, "GOOGLE/1", 300);
        entityManager.flush();
        entityManager.clear();

        // then
        InAppPurchase found = inAppPurchaseRepository.findById(purchase.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(InAppPurchaseStatus.REFUNDED);
        assertThat(found.getRefundedAt()).isEqualTo(refundedAt);
        assertThat(found.getRefundReason()).isEqualTo("GOOGLE/1");
        assertThat(found.getReclaimedCurrency()).isEqualTo(300);
    }

    @Test
    void findByTransactionIdAndPurchaseToken_ThenFindTheSamePurchase() {
        // given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        InAppPurchase purchase = inAppPurchaseRepository.save(
                purchase(user, PaymentPlatform.ANDROID, "piece_1000", "GPA.2", "token-2"));

        // when, then
        assertThat(inAppPurchaseRepository.findByTransactionId("GPA.2")).contains(purchase);
        assertThat(inAppPurchaseRepository.findFirstByPurchaseToken("token-2")).contains(purchase);
        assertThat(inAppPurchaseRepository.findByIdForUpdate(purchase.getId())).contains(purchase);
    }

    @Test
    void existsByUserIdAndProductIdAndStatusAndIdNot_ThenCountsOnlyOtherVerifiedPurchases() {
        // given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        InAppPurchase android = inAppPurchaseRepository.save(
                purchase(user, PaymentPlatform.ANDROID, "remove_ads", "GPA.3", "token-3"));
        InAppPurchase ios = inAppPurchaseRepository.save(
                purchase(user, PaymentPlatform.IOS, "remove_ads", "2000000000000003", null));

        // when
        boolean beforeRefund = inAppPurchaseRepository.existsByUserIdAndProductIdAndStatusAndIdNot(
                user.getId(), "remove_ads", InAppPurchaseStatus.VERIFIED, android.getId());
        ios.markRefunded(Instant.parse("2026-10-11T00:00:00Z"), "APPLE/0", 0);
        entityManager.flush();
        boolean afterRefund = inAppPurchaseRepository.existsByUserIdAndProductIdAndStatusAndIdNot(
                user.getId(), "remove_ads", InAppPurchaseStatus.VERIFIED, android.getId());

        // then
        assertThat(beforeRefund).isTrue();
        assertThat(afterRefund).isFalse();
    }

    private InAppPurchase purchase(UserEntity user, PaymentPlatform platform, String productId,
                                   String transactionId, String purchaseToken) {
        return InAppPurchase.builder()
                .user(user)
                .platform(platform)
                .productId(productId)
                .transactionId(transactionId)
                .purchaseToken(purchaseToken)
                .status(InAppPurchaseStatus.VERIFIED)
                .grantedCurrency("remove_ads".equals(productId) ? 0 : 1000)
                .build();
    }
}
