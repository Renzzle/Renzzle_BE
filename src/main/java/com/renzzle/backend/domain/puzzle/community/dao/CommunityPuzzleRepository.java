package com.renzzle.backend.domain.puzzle.community.dao;

import com.renzzle.backend.domain.puzzle.community.dao.query.CommunityPuzzleQueryRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface CommunityPuzzleRepository extends JpaRepository<CommunityPuzzle, Long>, CommunityPuzzleQueryRepository {

    @Query(value = "SELECT cp.* FROM community_puzzle cp " +
            "JOIN user_community_puzzle ucp ON ucp.community_id = cp.id " +
            "WHERE ucp.user_id = :userId AND ucp.is_liked = TRUE AND ucp.liked_at IS NOT NULL " +
            "AND (" +
            "   :cursorId IS NULL " +
            "   OR (ucp.liked_at, cp.id) < (SELECT ucp2.liked_at, cp2.id FROM user_community_puzzle ucp2 " +
            "                       JOIN community_puzzle cp2 ON ucp2.community_id = cp2.id " +
            "                       WHERE cp2.id = :cursorId AND ucp2.user_id = :userId)) " +
            "ORDER BY ucp.liked_at DESC, cp.id DESC " +
            "LIMIT :size", nativeQuery = true)
    List<CommunityPuzzle> getUserLikedPuzzles(@Param("userId") Long userId, @Param("cursorId") Long cursorId, @Param("size") int size);

    @Query(value = "SELECT cp.* FROM community_puzzle cp " +
            "WHERE cp.author_id = :userId " +
            "AND (" +
            "   :cursorId IS NULL " +
            "   OR (cp.created_at, cp.id) < (SELECT cp2.created_at, cp2.id FROM community_puzzle cp2 " +
            "                               WHERE cp2.id = :cursorId)" +
            ") " +
            "ORDER BY cp.created_at DESC, cp.id DESC " +
            "LIMIT :size", nativeQuery = true)
    List<CommunityPuzzle> getUserPuzzles(@Param("userId") Long userId, @Param("cursorId") Long cursorId, @Param("size") int size);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE CommunityPuzzle cp SET cp.status = (SELECT s FROM Status s WHERE s.name = 'DELETED'), " +
            "cp.deletedAt = :deletedAt WHERE cp.id = :puzzleId")
    int softDelete(@Param("puzzleId") Long puzzleId, @Param("deletedAt") Instant deletedAt);

    @Query("SELECT c FROM CommunityPuzzle c WHERE c.rating BETWEEN :min AND :max AND c.isVerified = true AND c.user <> :user")
    List<CommunityPuzzle> findAvailablePuzzlesForUser(@Param("min") double min, @Param("max") double max, @Param("user") UserEntity user);

    @Query("SELECT p FROM CommunityPuzzle p " +
            "WHERE p.boardStatus NOT IN (" +
            "    SELECT l.boardStatus FROM LatestRankPuzzle l WHERE l.user = :user" +
            ") AND p.isVerified = true " +
            "ORDER BY p.rating ASC")
    List<CommunityPuzzle> findAvailableCommunityPuzzlesSortedByRating(@Param("user") UserEntity user);

    @Query(value = "SELECT * FROM community_puzzle WHERE id = :id", nativeQuery = true)
    CommunityPuzzle findByIdIncludingDeleted(@Param("id") Long id);

    List<CommunityPuzzle> findByCreatedAtAfter(Instant after);

    List<CommunityPuzzle> findTop30ByCreatedAtBeforeOrderByCreatedAtDesc(Instant before);

    @Query("SELECT COUNT(p) FROM CommunityPuzzle p WHERE p.user.id = :userId")
    long countByAuthor(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(p.likeCount), 0) FROM CommunityPuzzle p WHERE p.user.id = :userId")
    int sumLikesByUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(p.dislikeCount), 0) FROM CommunityPuzzle p WHERE p.user.id = :userId")
    int sumDislikesByUser(@Param("userId") Long userId);

    @Query("SELECT DISTINCT p.user FROM CommunityPuzzle p WHERE p.createdAt >= :since")
    List<UserEntity> findUsersWhoCreatedPuzzlesSince(@Param("since") Instant since);

}
