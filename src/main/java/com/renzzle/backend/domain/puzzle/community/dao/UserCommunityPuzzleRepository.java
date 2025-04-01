package com.renzzle.backend.domain.puzzle.community.dao;

import com.renzzle.backend.domain.puzzle.community.domain.UserCommunityPuzzle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCommunityPuzzleRepository extends JpaRepository<UserCommunityPuzzle, Long> {

    Optional<UserCommunityPuzzle> findByUser_IdAndPuzzle_Id(Long userId, Long puzzleId);

    @Query("SELECT CASE WHEN EXISTS (" +
            "    SELECT 1 FROM UserCommunityPuzzle ucp " +
            "    WHERE ucp.user.id = :userId " +
            "    AND ucp.puzzle.id = :puzzleId " +
            "    AND ucp.isSolved = TRUE" +
            ") THEN TRUE ELSE FALSE END")
    boolean checkIsSolvedPuzzle(@Param("userId") Long userId, @Param("puzzleId") Long puzzleId);

    @Query("SELECT ucp.like " +
            "FROM UserCommunityPuzzle ucp " +
            "WHERE ucp.user.id = :userId " +
            "AND ucp.puzzle.id = :puzzleId")
    Optional<Boolean> getMyLike(@Param("userId") Long userId, @Param("puzzleId") Long puzzleId);

    @Query("SELECT ucp.dislike " +
            "FROM UserCommunityPuzzle ucp " +
            "WHERE ucp.user.id = :userId " +
            "AND ucp.puzzle.id = :puzzleId")
    Optional<Boolean> getMyDislike(@Param("userId") Long userId, @Param("puzzleId") Long puzzleId);

}
