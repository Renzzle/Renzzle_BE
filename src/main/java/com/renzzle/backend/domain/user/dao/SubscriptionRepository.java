package com.renzzle.backend.domain.user.dao;

import com.renzzle.backend.domain.user.domain.LikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    @Query("SELECT s FROM SubscriptionEntity s WHERE s.user.id = :userId AND (:id IS NULL OR s.id > :id) ORDER BY s.id ASC")
    List<SubscriptionEntity> findUserSubscriptions(@Param("userId") Long userId, @Param("id") Long id, @Param("size") int size);
}
