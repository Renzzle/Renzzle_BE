package com.renzzle.backend.domain.puzzle.community.dao;

import com.renzzle.backend.domain.puzzle.community.dao.projection.LikeDislikeProjection;
import com.renzzle.backend.domain.puzzle.community.domain.UserCommunityPuzzle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserCommunityPuzzleRepository extends JpaRepository<UserCommunityPuzzle, Long> {

    @Query("SELECT CASE WHEN EXISTS (" +
            "    SELECT 1 FROM UserCommunityPuzzle ucp " +
            "    WHERE ucp.user.id = :userId " +
            "    AND ucp.puzzle.id = :puzzleId " +
            "    AND ucp.isSolved = TRUE" +
            ") THEN TRUE ELSE FALSE END")
    boolean checkIsSolvedPuzzle(@Param("userId") Long userId, @Param("puzzleId") Long puzzleId);

    @Query("SELECT ucp.like AS like, ucp.dislike AS dislike " +
            "FROM UserCommunityPuzzle ucp " +
            "WHERE ucp.user.id = :userId AND ucp.puzzle.id = :puzzleId")
    Optional<LikeDislikeProjection> getMyLikeDislike(
            @Param("userId") Long userId,
            @Param("puzzleId") Long puzzleId);

    @Modifying
    @Query("UPDATE UserCommunityPuzzle ucp SET ucp.isSolved = TRUE, ucp.solvedAt = :solvedAt " +
            "WHERE ucp.user.id = :userId AND ucp.puzzle.id = :puzzleId")
    int solvePuzzle(@Param("userId") Long userId, @Param("puzzleId") Long puzzleId, @Param("solvedAt") Instant solvedAt);

}
