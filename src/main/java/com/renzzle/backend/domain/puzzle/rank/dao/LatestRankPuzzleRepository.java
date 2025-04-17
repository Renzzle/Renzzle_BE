package com.renzzle.backend.domain.puzzle.rank.dao;

import com.renzzle.backend.domain.puzzle.rank.domain.LatestRankPuzzle;
import com.renzzle.backend.domain.user.domain.UserEntity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LatestRankPuzzleRepository extends JpaRepository<LatestRankPuzzle, Long> {
    Optional<LatestRankPuzzle> findTopByUserOrderByAssignedAtDesc(UserEntity user);

    List<LatestRankPuzzle> findAllByUserOrderByAssignedAtAsc(UserEntity user);

    List<LatestRankPuzzle> findAllByUser(UserEntity user);

    @Query("SELECT DISTINCT l.user" +
    " FROM LatestRankPuzzle l" +
    " WHERE l.assignedAt >= :threshold")
    List<UserEntity> findActiveUsersWithinPeriod(@Param("threshold") Instant threshold);
}
