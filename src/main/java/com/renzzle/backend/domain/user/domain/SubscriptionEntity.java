package com.renzzle.backend.domain.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "subscription")
public class SubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 구독한 사용자 (구독자가 되는 사용자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // 구독된 사용자 (구독 당한 사람)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscribed_user_id", nullable = false)
    private UserEntity subscribedUser;

    // 구독 일시
    @Column(name = "subscribed_at", nullable = false, updatable = false)
    private Instant subscribedAt;

    @PrePersist
    protected void onSubscribe() {
        this.subscribedAt = Instant.now();
    }
}
