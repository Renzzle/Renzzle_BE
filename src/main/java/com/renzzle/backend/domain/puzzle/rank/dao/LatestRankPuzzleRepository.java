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
    // By id, not assignedAt: two assignments can share an instant
    Optional<LatestRankPuzzle> findTopByUserOrderByIdDesc(UserEntity user);

    List<LatestRankPuzzle> findAllByUserOrderByAssignedAtAsc(UserEntity user);

    List<LatestRankPuzzle> findAllByUser(UserEntity user);

    @Query("SELECT DISTINCT l.user FROM LatestRankPuzzle l WHERE l.assignedAt >= :threshold")
    List<UserEntity> findActiveUsersWithinPeriod(@Param("threshold") Instant threshold);
}
