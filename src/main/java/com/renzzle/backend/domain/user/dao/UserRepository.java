package com.renzzle.backend.domain.user.dao;

import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.user.domain.Title;
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

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserEntity u SET u.status = (SELECT s FROM Status s WHERE s.name = 'DELETED'), " +
            "u.deletedAt = :deletedAt WHERE u.id = :userId")
    int softDelete(@Param("userId") Long userId, @Param("deletedAt") Instant deletedAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserEntity u SET u.currency = u.currency + :amount WHERE u.id = :userId")
    void addUserCurrency(@Param("userId") Long userId, @Param("amount") int amount);

    @Query("SELECT CASE WHEN (u.lastAccessedAt < CURRENT_DATE) THEN true ELSE false END FROM UserEntity u WHERE u.id = :userId")
    Boolean isLastAccessBeforeToday(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserEntity u SET u.lastAccessedAt = :lastAccessedAt WHERE u.id = :userId")
    void updateLastAccessedAt(@Param("userId") Long userId, @Param("lastAccessedAt") Instant lastAccessedAt);

    @Query("SELECT u.title FROM UserEntity u WHERE u.id = :userId")
    Optional<Title> getUserTitle(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserEntity u SET u.title = :title WHERE u.id = :userId")
    void updateUserTitle(@Param("userId") Long userId, @Param("title") Title title);

    @Query(value = """
    SELECT (
        (SELECT u.rating FROM user u WHERE u.id = :userId) >= :minRating
        AND
        (SELECT COUNT(*) FROM community_puzzle cp WHERE cp.author_id = :userId AND cp.status != 'DELETED') >= :minPuzzleCount
        AND
        (SELECT COALESCE(SUM(cp.like_count), 0) FROM community_puzzle cp WHERE cp.author_id = :userId AND cp.status != 'DELETED') >= :minLikes
        AND
        (SELECT COALESCE(SUM(cp.solved_count), 0) FROM community_puzzle cp WHERE cp.author_id = :userId AND cp.status != 'DELETED') >= :minSolverCount
    ) AS result
    """, nativeQuery = true)
    Long isUserQualifiedRaw(
            @Param("userId") Long userId,
            @Param("minLikes") int minLikes,
            @Param("minPuzzleCount") int minPuzzleCount,
            @Param("minRating") double minRating,
            @Param("minSolverCount") int minSolverCount
    );

    default boolean isUserQualified(Long userId, int minLikes, int minPuzzleCount, double minRating, int minSolverCount) {
        return isUserQualifiedRaw(userId, minLikes, minPuzzleCount, minRating, minSolverCount) == 1L;
    }

    List<UserEntity> findTop100ByOrderByRatingDesc();
           
    //Ranking
    @Query("SELECT CASE WHEN (u.lastAccessedAt < CURRENT_DATE) THEN true ELSE false END FROM UserEntity u WHERE u.id = :userId")
    Boolean isLastAccessBeforeToday(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserEntity u SET u.lastAccessedAt = :lastAccessedAt WHERE u.id = :userId")
    void updateLastAccessedAt(@Param("userId") Long userId, @Param("lastAccessedAt") Instant lastAccessedAt);

    @Query("SELECT u.title FROM UserEntity u WHERE u.id = :userId")
    Optional<Title> getUserTitle(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserEntity u SET u.title = :title WHERE u.id = :userId")
    void updateUserTitle(@Param("userId") Long userId, @Param("title") Title title);

    @Query(value = "SELECT * FROM user WHERE id = :id", nativeQuery = true)
    UserEntity findByIdIncludingDeleted(@Param("id") Long id);

}
