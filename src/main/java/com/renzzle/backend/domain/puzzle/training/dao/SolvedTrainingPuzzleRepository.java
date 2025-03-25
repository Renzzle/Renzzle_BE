package com.renzzle.backend.domain.puzzle.training.dao;

import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.puzzle.training.domain.SolvedTrainingPuzzle;
import com.renzzle.backend.domain.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SolvedTrainingPuzzleRepository extends JpaRepository<SolvedTrainingPuzzle, Long> {

    @Query(value = "SELECT * FROM solved_training_puzzle " +
            "WHERE user_id = :userId AND training_id = :puzzleId",
            nativeQuery = true)
    Optional<SolvedTrainingPuzzle> findByUserIdAndPuzzleId(@Param("userId") Long userId,
                                                           @Param("puzzleId") Long puzzleId);

    boolean existsByUserAndPuzzle(UserEntity user, TrainingPuzzle puzzle);

    @Query(value = "SELECT COUNT(*) " +
            "FROM solved_lesson_puzzle sl " +
            "JOIN lesson_puzzle l ON sl.lesson_id = l.id " +
            "WHERE sl.user_id = :userId AND l.chapter = :chapter",
            nativeQuery = true)
    int countSolvedLesson(@Param("userId") Long userId,
                          @Param("chapter") int chapter);

}
