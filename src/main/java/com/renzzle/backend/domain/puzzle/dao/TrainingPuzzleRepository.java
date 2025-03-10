package com.renzzle.backend.domain.puzzle.dao;

import com.renzzle.backend.domain.puzzle.domain.TrainingPuzzle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface TrainingPuzzleRepository extends JpaRepository<TrainingPuzzle, Long> {

    @Query(value = "SELECT COALESCE(MAX(lesson_index), -1) " +
            "FROM lesson_puzzle " +
            "WHERE chapter = :chapter",
            nativeQuery = true)
    int findTopIndex(@Param("chapter") int chapter);


    @Query(value = "SELECT COALESCE(MAX(l.lesson_index), -1) " +
            "FROM lesson_puzzle l " +
            "JOIN solved_lesson_puzzle sl ON l.id = sl.lesson_id " +
            "WHERE sl.user_id = :userId AND l.chapter = :chapter",
            nativeQuery = true)
    int findTopSolvedPuzzleIndex(@Param("userId") Long userId,
                                 @Param("chapter") int chapter);

    @Modifying
    @Transactional
    @Query(value = "UPDATE lesson_puzzle " +
            "SET lesson_index = lesson_index + 1 " +
            "WHERE lesson_index >= :targetIdx AND chapter = :chapter",
            nativeQuery = true)
    void increaseIndexesFrom(@Param("chapter") int chapter,
                             @Param("targetIdx") int targetIdx);

    @Modifying
    @Transactional
    @Query(value = "UPDATE lesson_puzzle " +
            "SET lesson_index = lesson_index - 1 " +
            "WHERE lesson_index > :targetIdx AND chapter = :chapter",
            nativeQuery = true)
    void decreaseIndexesFrom(@Param("chapter") int chapter,
                             @Param("targetIdx") int targetIdx);

    @Query(value = "SELECT * FROM lesson_puzzle " +
            "WHERE chapter = :chapter AND lesson_index = :index",
            nativeQuery = true)
    Optional<TrainingPuzzle> findByChapterAndIndex(@Param("chapter") int chapter,
                                                   @Param("index") int index);

    @Query(value = "SELECT COUNT(*) FROM lesson_puzzle " +
            "WHERE chapter = :chapter",
            nativeQuery = true)
    int countAllLessonByChapter(@Param("chapter") int chapter);

    Page<TrainingPuzzle> findByChapter(int chapter, Pageable pageable);

}
