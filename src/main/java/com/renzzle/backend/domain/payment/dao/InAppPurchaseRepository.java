package com.renzzle.backend.domain.payment.dao;

import com.renzzle.backend.domain.payment.domain.InAppPurchase;
import com.renzzle.backend.domain.payment.domain.InAppPurchaseStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InAppPurchaseRepository extends JpaRepository<InAppPurchase, Long> {

    boolean existsByPurchaseToken(String purchaseToken);

    boolean existsByTransactionId(String transactionId);

    Optional<InAppPurchase> findByTransactionId(String transactionId);

    Optional<InAppPurchase> findFirstByPurchaseToken(String purchaseToken);

    List<InAppPurchase> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM InAppPurchase p WHERE p.id = :id")
    Optional<InAppPurchase> findByIdForUpdate(@Param("id") Long id);

    boolean existsByUserIdAndProductIdAndStatusAndIdNot(
            Long userId, String productId, InAppPurchaseStatus status, Long id);
}
