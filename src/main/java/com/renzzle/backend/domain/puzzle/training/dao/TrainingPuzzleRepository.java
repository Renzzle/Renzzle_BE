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
            "WHERE training_index >= :targetIdx AND pack_id = :packId " +
            "ORDER BY training_index DESC",
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

    List<TrainingPuzzle> findByPack_IdOrderByTrainingIndex(Long packId);

    void deleteAllByPack_Id(Long packId);

    @Query("SELECT p FROM TrainingPuzzle p " +
            "WHERE p.boardStatus NOT IN (" +
            "    SELECT l.boardStatus FROM LatestRankPuzzle l WHERE l.user = :user" +
            ") " +
            "ORDER BY p.rating ASC")
    List<TrainingPuzzle> findAvailableTrainingPuzzlesSortedByRating(@Param("user") UserEntity user);

    @Query("SELECT p.rankAttemptCount FROM TrainingPuzzle p WHERE p.id = :id")
    Optional<Integer> findRankAttemptCountById(@Param("id") Long id);

    // One statement, so concurrent results on the same puzzle add up instead of overwriting each other
    @Modifying
    @Transactional
    @Query(value = "UPDATE training_puzzle " +
            "SET rating = LEAST(GREATEST(rating + :delta, :minRating), :maxRating), " +
            "rank_attempt_count = rank_attempt_count + 1 " +
            "WHERE id = :id",
            nativeQuery = true)
    int applyRankResult(@Param("id") Long id,
                        @Param("delta") double delta,
                        @Param("minRating") double minRating,
                        @Param("maxRating") double maxRating);
}
