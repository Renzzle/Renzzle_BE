package com.renzzle.backend.domain.puzzle.training.dao;

import com.renzzle.backend.domain.puzzle.training.dao.projection.PackLastSolvedProjection;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.puzzle.training.domain.SolvedTrainingPuzzle;
import com.renzzle.backend.domain.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SolvedTrainingPuzzleRepository extends JpaRepository<SolvedTrainingPuzzle, Long> {

    @Query(value = "SELECT * FROM solved_training_puzzle " +
            "WHERE user_id = :userId AND training_id = :puzzleId",
            nativeQuery = true)
    Optional<SolvedTrainingPuzzle> findByUserIdAndPuzzleId(@Param("userId") Long userId,
                                                           @Param("puzzleId") Long puzzleId);

    boolean existsByUserAndPuzzle(UserEntity user, TrainingPuzzle puzzle);

    @Query("SELECT s FROM SolvedTrainingPuzzle s " +
            "JOIN FETCH s.puzzle p " +
            "JOIN FETCH p.pack pack " +
            "WHERE s.user.id = :userId AND pack.isPublic = true " +
            "ORDER BY s.solvedAt DESC LIMIT 1")
    Optional<SolvedTrainingPuzzle> findLatestInPublicPackByUserId(@Param("userId") Long userId);

    // When the user last solved a puzzle in each pack
    @Query("SELECT p.pack.id AS packId, MAX(s.solvedAt) AS lastSolvedAt " +
            "FROM SolvedTrainingPuzzle s JOIN s.puzzle p " +
            "WHERE s.user.id = :userId GROUP BY p.pack.id")
    List<PackLastSolvedProjection> findLastSolvedAtByPack(@Param("userId") Long userId);

    List<SolvedTrainingPuzzle> findAllByPuzzleId(Long puzzleId);

    void deleteAllByPuzzle_Pack_Id(Long packId);


}
