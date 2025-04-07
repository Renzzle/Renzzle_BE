package com.renzzle.backend.domain.puzzle.training.dao;

import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.user.domain.UserEntity;
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

    @Query("SELECT p FROM TrainingPuzzle p " +
            "WHERE p.rating BETWEEN :minRating AND :maxRating " +
            "AND p.boardStatus NOT IN (" +
            "   SELECT lp.boardStatus FROM LatestRankPuzzle lp WHERE lp.user = :user" +
            ")")
    List<TrainingPuzzle> findAvailablePuzzlesForUser(
            @Param("minRating") double minRating,
            @Param("maxRating") double maxRating,
            @Param("user") UserEntity user
    );
}
