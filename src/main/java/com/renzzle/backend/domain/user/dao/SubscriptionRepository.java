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

//<<<<<<< Updated upstream
    @Query("SELECT s FROM SubscriptionEntity s WHERE s.subscriber.id = :userId AND (:id IS NULL OR s.id >= :id) ORDER BY s.id ASC")
    List<SubscriptionEntity> findUserSubscriptions(@Param("userId") Long userId, @Param("id") Long id, Pageable pageable);
////=======
////    @Query("SELECT s FROM SubscriptionEntity s WHERE s.subscriber.id = :userId AND (:id IS NULL OR s.id >= :id) ORDER BY s.id ASC")
////    List<SubscriptionEntity> findUserSubscriptions(@Param("userId") Long userId, @Param("id") Long id, Pageable pageable);
//
    // 첫 번째 페이지 조회
    @Query(value = "SELECT s FROM subscription " +
            "WHERE subscriber.id = :userId " +
            "ORDER BY id ASC " +
            "LIMIT :size"
            , nativeQuery = true)
    List<SubscriptionEntity> findFirstUserSubscriptions(@Param("userId") Long userId, @Param("size") int size);

    // 특정 ID 이후의 페이지 조회
    @Query(value = "SELECT * FROM subscription " +
            "WHERE subscriber_id = :userId AND id > :id " +
            "ORDER BY id ASC " +
            "LIMIT :size",
            nativeQuery = true)
    List<SubscriptionEntity> findUserSubscriptionsFromId(@Param("userId") Long userId,
                                                         @Param("id") Long id,
                                                         @Param("size") int size);
//>>>>>>> Stashed changes


    @Query(value = "SELECT * FROM subscription " +
            "WHERE subscriber_id = :subscriberId AND subscribedTo_id = :subscribedToId",
            nativeQuery = true)
    Optional<SubscriptionEntity> findBySubscriberAndSubscribedTo(@Param("subscriberId") Long subscriberId,
                                                                 @Param("subscribedToId") Long subscribedToId);

    @Modifying
    @Query(value = "DELETE FROM subscription WHERE subscriber_id = :userId OR subscribedTo_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Long userId);
}
