package com.renzzle.backend.domain.puzzle.dao;

import com.renzzle.backend.domain.puzzle.domain.SolvedLessonPuzzle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolvedLessonPuzzleRepository extends JpaRepository<SolvedLessonPuzzle, Long> {
}
