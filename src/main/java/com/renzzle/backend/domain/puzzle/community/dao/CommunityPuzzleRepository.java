package com.renzzle.backend.domain.puzzle.community.dao;

import com.renzzle.backend.domain.puzzle.community.dao.query.CommunityPuzzleQueryRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
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
            "ORDER BY cp.created_at DESC, cp.id DESC " +
            "LIMIT :size", nativeQuery = true)
    List<CommunityPuzzle> getUserPuzzles(@Param("userId") Long userId, @Param("cursorId") Long cursorId, @Param("size") int size);

    @Modifying
    @Query("UPDATE CommunityPuzzle cp SET cp.status = (SELECT s FROM Status s WHERE s.name = 'DELETED'), " +
            "cp.deletedAt = :deletedAt WHERE cp.id = :puzzleId")
    int softDelete(@Param("puzzleId") Long puzzleId, @Param("deletedAt") Instant deletedAt);

}
