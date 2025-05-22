package com.renzzle.backend.domain.puzzle.training.dao;

import com.renzzle.backend.domain.puzzle.training.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.training.domain.Pack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PackRepository extends JpaRepository<Pack, Long> {

    @Modifying
    @Transactional
    @Query(value = "UPDATE pack " +
            "SET puzzle_count = puzzle_count + 1 " +
            "WHERE id = :packId",
            nativeQuery = true)
    void increasePuzzleCount(@Param("packId") Long packId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE pack " +
            "SET puzzle_count = puzzle_count - 1 " +
            "WHERE id = :packId",
            nativeQuery = true)
    void decreasePuzzleCount(@Param("packId") Long packId);

    List<Pack> findByDifficulty(Difficulty difficulty);

    Optional<Pack> findFirstByOrderByIdAsc();
}
