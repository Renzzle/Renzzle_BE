package com.renzzle.backend.domain.payment.domain;

import com.renzzle.backend.domain.user.domain.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
        name = "in_app_purchase",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_purchase_token", columnNames = "purchase_token"),
                @UniqueConstraint(name = "uk_transaction_id", columnNames = "transaction_id")
        }
)
public class InAppPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 10)
    private PaymentPlatform platform;

    @Column(name = "product_id", nullable = false, length = 100)
    private String productId;

    @Column(name = "transaction_id", length = 512)
    private String transactionId;

    @Column(name = "purchase_token", length = 512)
    private String purchaseToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InAppPurchaseStatus status;

    @Column(name = "granted_currency", nullable = false)
    private int grantedCurrency;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
