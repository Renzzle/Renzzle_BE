package com.renzzle.backend.domain.puzzle.community.dao;

import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPuzzleRepository extends JpaRepository<CommunityPuzzle, Long>, CommunityPuzzleQueryRepository {

    @Query("SELECT CASE WHEN EXISTS (" +
            "    SELECT 1 FROM UserCommunityPuzzle ucp " +
            "    WHERE ucp.user.id = :userId " +
            "    AND ucp.puzzle.id = :puzzleId " +
            "    AND ucp.isSolved = TRUE" +
            ") THEN TRUE ELSE FALSE END")
    boolean checkIsSolvedPuzzle(@Param("userId") Long userId, @Param("puzzleId") Long puzzleId);

}
