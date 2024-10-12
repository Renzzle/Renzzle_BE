package com.renzzle.backend.domain.puzzle.dao;

import com.renzzle.backend.domain.puzzle.domain.LessonPuzzle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface LessonPuzzleRepository extends JpaRepository<LessonPuzzle, Long> {

    @Query(value = "SELECT COALESCE(MAX(lesson_index), -1) " +
            "FROM lesson_puzzle " +
            "WHERE chapter = :chapter",
            nativeQuery = true)
    int findTopIndex(int chapter);

    @Modifying
    @Transactional
    @Query(value = "UPDATE lesson_puzzle " +
            "SET lesson_index = lesson_index + 1 " +
            "WHERE lesson_index >= :targetIdx AND chapter = :chapter",
            nativeQuery = true)
    void increaseIndexesFrom(int chapter, int targetIdx);

    @Query(value = "SELECT * FROM lesson_puzzle " +
            "WHERE chapter = :chapter AND lesson_index = :index",
            nativeQuery = true)
    Optional<LessonPuzzle> findByChapterAndIndex(int chapter, int index);

    @Query(value = "SELECT COUNT(*) FROM lesson_puzzle " +
            "WHERE chapter = :chapter",
            nativeQuery = true)
    int countAllLessonByChapter(int chapter);

    Page<LessonPuzzle> findByChapter(int chapter, Pageable pageable);

}
