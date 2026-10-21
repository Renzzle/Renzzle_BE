package com.renzzle.backend.domain.payment.service;

import com.renzzle.backend.domain.payment.api.response.GetGoogleVoidedPurchaseForAdminResponse;
import com.renzzle.backend.domain.payment.api.response.GetPurchaseForAdminResponse;
import com.renzzle.backend.domain.payment.dao.InAppPurchaseRepository;
import com.renzzle.backend.domain.payment.domain.InAppPurchase;
import com.renzzle.backend.domain.payment.domain.InAppPurchaseStatus;
import com.renzzle.backend.domain.payment.domain.PaymentPlatform;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.support.DataJpaTestWithInitContainers;
import com.renzzle.backend.support.TestUserEntityBuilder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static com.renzzle.backend.global.common.constant.StringConstant.DELETED_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// A real database, because the purchase's lazy user cannot be loaded once that user has withdrawn
@DataJpaTestWithInitContainers
class PaymentAdminServiceRepositoryTest {

    private static final String ORDER_ID = "GPA.WITHDRAWN";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InAppPurchaseRepository inAppPurchaseRepository;

    private final GooglePlayClient googlePlayClient = mock(GooglePlayClient.class);

    private PaymentAdminService paymentAdminService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        GoogleVoidedPurchaseService googleVoidedPurchaseService = new GoogleVoidedPurchaseService(
                googlePlayClient, inAppPurchaseRepository, mock(RefundService.class), true, "");
        paymentAdminService = new PaymentAdminService(inAppPurchaseRepository, userRepository,
                new RefundPolicy(Clock.systemUTC(), 7), googleVoidedPurchaseService,
                mock(AppleAppStoreServerClient.class));

        user = TestUserEntityBuilder.builder().withCurrency(1000).save(userRepository);
        inAppPurchaseRepository.save(InAppPurchase.builder()
                .user(user)
                .platform(PaymentPlatform.ANDROID)
                .productId("piece_1000")
                .transactionId(ORDER_ID)
                .purchaseToken("token-withdrawn")
                .status(InAppPurchaseStatus.VERIFIED)
                .grantedCurrency(1000)
                .build());
        entityManager.flush();
        userRepository.softDelete(user.getId(), Instant.now());
        entityManager.clear();
    }

    @Test
    void searchPurchasesForAdmin_WhenBuyerWithdrew_ThenStillShowsThePurchase() {
        // when
        List<GetPurchaseForAdminResponse> result = paymentAdminService.searchPurchasesForAdmin(ORDER_ID);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(user.getId());
        assertThat(result.get(0).email()).isEqualTo(user.getEmail());
        assertThat(result.get(0).nickname()).isEqualTo(DELETED_USER);
    }

    @Test
    void getGoogleVoidedPurchasesForAdmin_WhenBuyerWithdrew_ThenListsTheRefundWithNothingToTakeBack() {
        // given
        when(googlePlayClient.listVoidedPurchases(null)).thenReturn(new GoogleVoidedPurchasePage(
                List.of(new GoogleVoidedPurchase("token-withdrawn", ORDER_ID, Instant.now().toEpochMilli(), 0, 1)),
                null));

        // when
        List<GetGoogleVoidedPurchaseForAdminResponse> result = paymentAdminService.getGoogleVoidedPurchasesForAdmin();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).matched()).isTrue();
        assertThat(result.get(0).userId()).isEqualTo(user.getId());
        assertThat(result.get(0).nickname()).isEqualTo(DELETED_USER);
        assertThat(result.get(0).reclaimCurrency()).isZero();
    }
}
