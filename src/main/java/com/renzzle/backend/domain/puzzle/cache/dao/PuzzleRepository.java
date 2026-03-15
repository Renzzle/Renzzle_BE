package com.renzzle.backend.domain.puzzle.cache.dao;

import com.renzzle.backend.domain.puzzle.cache.domain.Puzzle;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PuzzleRepository extends JpaRepository<Puzzle, Long> {

    Optional<Puzzle> findByPuzzleTypeAndSourceId(PuzzleType puzzleType, Long sourceId);
}
