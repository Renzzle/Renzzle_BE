package com.renzzle.backend.domain.puzzle.play.infrastructure;

import com.renzzle.backend.domain.puzzle.play.domain.Puzzle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PuzzleRepository extends JpaRepository<Puzzle, Long> {
}
