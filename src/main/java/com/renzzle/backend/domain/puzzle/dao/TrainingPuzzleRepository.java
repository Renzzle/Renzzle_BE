package com.renzzle.backend.domain.puzzle.dao;

import com.renzzle.backend.domain.puzzle.domain.TrainingPuzzle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TrainingPuzzleRepository extends JpaRepository<TrainingPuzzle, Long> {

    @Query(value = "SELECT COALESCE(MAX(training_index), -1) " +
            "FROM training_puzzle " +
            "WHERE pack_id = :packId",
            nativeQuery = true)
    int findTopIndex(@Param("packId") Long packId);


//    @Query(value = "SELECT COALESCE(MAX(t.lesson_index), -1) " +
//            "FROM training_puzzle t " +
//            "JOIN solved_training_puzzle st ON t.id = st.training_id " +
//            "WHERE st.user_id = :userId AND t.chapter = :chapter",
//            nativeQuery = true)
//    int findTopSolvedPuzzleIndex(@Param("userId") Long userId,
//                                 @Param("chapter") int chapter);

    @Modifying
    @Transactional
    @Query(value = "UPDATE training_puzzle " +
            "SET training_index = training_index + 1 " +
            "WHERE training_index >= :targetIdx AND pack_id = :packId",
            nativeQuery = true)
    void increaseIndexesFrom(@Param("packId") Long packId,
                             @Param("targetIdx") int targetIdx);

    @Modifying
    @Transactional
    @Query(value = "UPDATE training_puzzle " +
            "SET training_index = training_index - 1 " +
            "WHERE training_index > :targetIdx",
            nativeQuery = true)
    void decreaseIndexesFrom(@Param("targetIdx") int targetIdx);

    @Query(value = "SELECT * FROM training_puzzle " +
            "WHERE chapter = :chapter AND training_index = :index",
            nativeQuery = true)
    Optional<TrainingPuzzle> findByChapterAndIndex(@Param("chapter") int chapter,
                                                   @Param("index") int index);

    @Query(value = "SELECT COUNT(*) FROM training_puzzle " +
            "WHERE chapter = :chapter",
            nativeQuery = true)
    int countAllTrainingByChapter(@Param("chapter") int chapter);


    List<TrainingPuzzle> findByPack_Id(Long packId);

    @Query(value = "SELECT * FROM training_puzzle " +
            "WHERE rating BETWEEN :minRating AND :maxRating ORDER BY RAND() LIMIT 1",
            nativeQuery = true)
    Optional<TrainingPuzzle> findRandomByRatingBetween(@Param("minRating") double minRating,
                                                       @Param("maxRating") double maxRating);

}
