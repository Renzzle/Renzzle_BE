package com.renzzle.backend.domain.puzzle.dao;

import com.renzzle.backend.domain.puzzle.domain.CommunityPuzzle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPuzzleRepository extends JpaRepository<CommunityPuzzle, Long> {
}
