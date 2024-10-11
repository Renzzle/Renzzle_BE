package com.renzzle.backend.domain.puzzle.dao;

import com.renzzle.backend.domain.puzzle.domain.SolvedLessonPuzzle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SolvedLessonPuzzleRepository extends JpaRepository<SolvedLessonPuzzle, Long> {

    @Query(value = "SELECT * FROM solved_lesson_puzzle " +
            "WHERE user_id = :userId AND lesson_id = :lessonId",
            nativeQuery = true)
    Optional<SolvedLessonPuzzle> findByUserIdAndLessonId(Long userId, Long lessonId);

}
