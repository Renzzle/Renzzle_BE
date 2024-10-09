package com.renzzle.backend.domain.puzzle.dao;

import com.renzzle.backend.domain.puzzle.domain.LessonPuzzle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonPuzzleRepository extends JpaRepository<LessonPuzzle, Long> {
}
