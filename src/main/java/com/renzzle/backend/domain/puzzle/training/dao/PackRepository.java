package com.renzzle.backend.domain.puzzle.training.dao;

import com.renzzle.backend.domain.puzzle.shared.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.training.domain.Pack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PackRepository extends JpaRepository<Pack, Long> {

    @Modifying
    @Transactional
    @Query(value = "UPDATE pack " +
            "SET puzzle_count = puzzle_count + 1 " +
            "WHERE pack_id = :pack_id",
            nativeQuery = true)
    void increasePuzzleCount(@Param("pack_id") Long packId);

    List<Pack> findByDifficulty(Difficulty difficulty);
}
