package com.renzzle.backend.domain.puzzle.cache.dao;

import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleCache;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PuzzleCacheRepository extends JpaRepository<PuzzleCache, Long> {

    Optional<PuzzleCache> findByPuzzleTypeAndPuzzleId(PuzzleType puzzleType, Long puzzleId);
}
