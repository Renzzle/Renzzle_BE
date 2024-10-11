package com.renzzle.backend.domain.user.dao;

import com.renzzle.backend.domain.user.domain.SubscriptionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

//    @Query("SELECT s FROM SubscriptionEntity s WHERE s.subscriber.id = :userId AND (:id IS NULL OR s.id > :id) ORDER BY s.id ASC")
//    List<SubscriptionEntity> findUserSubscriptions(@Param("userId") Long userId, @Param("id") Long id, @Param("size") int size);

    @Query("SELECT s FROM SubscriptionEntity s WHERE s.subscriber.id = :userId AND (:id IS NULL OR s.id >= :id) ORDER BY s.id ASC")
    List<SubscriptionEntity> findUserSubscriptions(@Param("userId") Long userId, @Param("id") Long id, Pageable pageable);

    @Query("SELECT s FROM SubscriptionEntity s WHERE s.subscriber.id = :subscriberId AND s.subscribedTo.id = :subscribedToId")
    Optional<SubscriptionEntity> findBySubscriberAndSubscribedTo(@Param("subscriberId") Long subscriberId, @Param("subscribedToId") Long subscribedToId);

    @Modifying
    @Query("DELETE FROM SubscriptionEntity s WHERE s.subscriber.id = :userId OR s.subscribedTo.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
