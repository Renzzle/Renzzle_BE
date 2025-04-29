package com.renzzle.backend.domain.user.dao;

import com.renzzle.backend.domain.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByDeviceId(String deviceId);

    Optional<UserEntity> findByEmail(String email);

    @Modifying
    @Query("UPDATE UserEntity u SET u.status = (SELECT s FROM Status s WHERE s.name = 'DELETED'), " +
            "u.deletedAt = :deletedAt WHERE u.id = :userId")
    int softDelete(@Param("userId") Long userId, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query("UPDATE UserEntity u SET u.currency = u.currency + :amount WHERE u.id = :userId")
    void addUserCurrency(@Param("userId") Long userId, @Param("amount") int amount);

    @Query("SELECT CASE WHEN (u.lastAccessedAt < CURRENT_DATE) THEN true ELSE false END FROM UserEntity u WHERE u.id = :userId")
    Boolean isLastAccessBeforeToday(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserEntity u SET u.lastAccessedAt = :lastAccessedAt WHERE u.id = :userId")
    void updateLastAccessedAt(@Param("userId") Long userId, @Param("lastAccessedAt") Instant lastAccessedAt);

}
