package com.renzzle.backend.domain.puzzle.rank.dao;

import com.renzzle.backend.domain.puzzle.rank.domain.LatestRankPuzzle;
import com.renzzle.backend.domain.user.domain.UserEntity;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LatestRankPuzzleRepository extends JpaRepository<LatestRankPuzzle, Long> {
    // Ordered by id, not assignedAt: two puzzles can share an assignment instant, and a tie there
    // would let a result be recorded against the wrong assignment.
    Optional<LatestRankPuzzle> findTopByUserOrderByIdDesc(UserEntity user);

    List<LatestRankPuzzle> findAllByUserOrderByAssignedAtAsc(UserEntity user);

    List<LatestRankPuzzle> findAllByUser(UserEntity user);

    @Query("SELECT DISTINCT l.user FROM LatestRankPuzzle l WHERE l.assignedAt >= :threshold")
    List<UserEntity> findActiveUsersWithinPeriod(@Param("threshold") Instant threshold);
}
