package com.renzzle.backend.domain.puzzle.community.dao;

import com.renzzle.backend.domain.puzzle.community.dao.projection.AuthorStatsProjection;
import com.renzzle.backend.domain.puzzle.community.dao.projection.CommunityBoardKeyProjection;
import com.renzzle.backend.domain.puzzle.community.dao.query.CommunityPuzzleQueryRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.shared.dao.projection.AnswerKeyProjection;
import com.renzzle.backend.domain.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommunityPuzzleRepository extends JpaRepository<CommunityPuzzle, Long>, CommunityPuzzleQueryRepository {

    @Query(value = "SELECT cp.* FROM community_puzzle cp " +
            "JOIN user_community_puzzle ucp ON ucp.community_id = cp.id " +
            "WHERE cp.status != 'DELETED' AND ucp.user_id = :userId AND ucp.is_liked = TRUE AND ucp.liked_at IS NOT NULL " +
            "AND (" +
            "   :cursorId IS NULL " +
            "   OR (ucp.liked_at, cp.id) < (SELECT ucp2.liked_at, cp2.id FROM user_community_puzzle ucp2 " +
            "                       JOIN community_puzzle cp2 ON ucp2.community_id = cp2.id " +
            "                       WHERE cp2.id = :cursorId AND ucp2.user_id = :userId)) " +
            "ORDER BY ucp.liked_at DESC, cp.id DESC " +
            "LIMIT :size", nativeQuery = true)
    List<CommunityPuzzle> getUserLikedPuzzles(@Param("userId") Long userId, @Param("cursorId") Long cursorId, @Param("size") int size);

    @Query(value = "SELECT cp.* FROM community_puzzle cp " +
            "WHERE cp.status != 'DELETED' AND cp.author_id = :userId " +
            "AND (" +
            "   :cursorId IS NULL " +
            "   OR (cp.created_at, cp.id) < (SELECT cp2.created_at, cp2.id FROM community_puzzle cp2 " +
            "                               WHERE cp2.id = :cursorId)" +
            ") " +
            "ORDER BY cp.created_at DESC, cp.id DESC " +
            "LIMIT :size", nativeQuery = true)
    List<CommunityPuzzle> getUserPuzzles(@Param("userId") Long userId, @Param("cursorId") Long cursorId, @Param("size") int size);

    @Modifying
    @Query("UPDATE CommunityPuzzle cp SET cp.status = (SELECT s FROM Status s WHERE s.name = 'DELETED'), " +
            "cp.deletedAt = :deletedAt WHERE cp.id = :puzzleId")
    int softDelete(@Param("puzzleId") Long puzzleId, @Param("deletedAt") Instant deletedAt);

    @Query("SELECT p FROM CommunityPuzzle p " +
            "WHERE p.boardStatus NOT IN (" +
            "    SELECT l.boardStatus FROM LatestRankPuzzle l WHERE l.user = :user" +
            ") AND p.isVerified = true " +
            "ORDER BY p.rating ASC")
    List<CommunityPuzzle> findAvailableCommunityPuzzlesSortedByRating(@Param("user") UserEntity user);

    List<CommunityPuzzle> findByRankAttemptCount(int rankAttemptCount);

    @Query("SELECT p.id FROM CommunityPuzzle p WHERE p.boardKey = :boardKey")
    Optional<Long> findIdByBoardKey(@Param("boardKey") String boardKey);

    List<AnswerKeyProjection> findByAnswerKey(String answerKey);

    @Query("SELECT p.rankAttemptCount FROM CommunityPuzzle p WHERE p.id = :id")
    Optional<Integer> findRankAttemptCountById(@Param("id") Long id);

    // Single statement so concurrent results add up instead of overwriting
    @Modifying
    @Transactional
    @Query(value = "UPDATE community_puzzle " +
            "SET rating = LEAST(GREATEST(rating + :delta, :minRating), :maxRating), " +
            "rank_attempt_count = rank_attempt_count + 1 " +
            "WHERE id = :id",
            nativeQuery = true)
    int applyRankResult(@Param("id") Long id,
                        @Param("delta") double delta,
                        @Param("minRating") double minRating,
                        @Param("maxRating") double maxRating);

    @Query(value = "SELECT * FROM community_puzzle WHERE id = :id", nativeQuery = true)
    CommunityPuzzle findByIdIncludingDeleted(@Param("id") Long id);

    @Query(value = "SELECT id, board_status AS boardStatus, board_key AS boardKey, status, deleted_at AS deletedAt " +
            "FROM community_puzzle", nativeQuery = true)
    List<CommunityBoardKeyProjection> findAllBoardKeysIncludingDeleted();

    // Counters change in single statements, so concurrent views and votes add up instead of overwriting
    @Modifying
    @Transactional
    @Query("UPDATE CommunityPuzzle cp SET cp.view = cp.view + 1 WHERE cp.id = :puzzleId")
    void increaseView(@Param("puzzleId") Long puzzleId);

    @Modifying
    @Transactional
    @Query("UPDATE CommunityPuzzle cp SET cp.solvedCount = cp.solvedCount + 1 WHERE cp.id = :puzzleId")
    void increaseSolvedCount(@Param("puzzleId") Long puzzleId);

    @Modifying
    @Transactional
    @Query("UPDATE CommunityPuzzle cp SET cp.likeCount = cp.likeCount + :likeDelta, " +
            "cp.dislikeCount = cp.dislikeCount + :dislikeDelta WHERE cp.id = :puzzleId")
    void addVoteCounts(@Param("puzzleId") Long puzzleId,
                       @Param("likeDelta") int likeDelta,
                       @Param("dislikeDelta") int dislikeDelta);

    // Native, so it reaches soft-deleted puzzles as well
    @Modifying
    @Transactional
    @Query(value = "UPDATE community_puzzle SET board_key = :boardKey WHERE id = :id", nativeQuery = true)
    void updateBoardKey(@Param("id") Long id, @Param("boardKey") String boardKey);

    @Query("SELECT p.id AS id, p.boardStatus AS boardStatus, p.answer AS answer, p.answerKey AS answerKey " +
            "FROM CommunityPuzzle p")
    List<AnswerKeyProjection> findAllAnswerKeys();

    @Modifying
    @Transactional
    @Query(value = "UPDATE community_puzzle SET answer_key = :answerKey WHERE id = :id", nativeQuery = true)
    void updateAnswerKey(@Param("id") Long id, @Param("answerKey") String answerKey);

    // Trend candidates: verified, not deleted, and not disliked more than liked
    String TREND_FILTER = "cp.status != 'DELETED' AND cp.is_verified = TRUE AND cp.like_count >= cp.dislike_count ";
    // Net likes halve every half-life, so a newer puzzle can overtake an older one with more likes
    String TREND_ORDER = "ORDER BY (cp.like_count - cp.dislike_count) "
            + "* POW(0.5, TIMESTAMPDIFF(SECOND, cp.created_at, :now) / :halfLifeSeconds) DESC, "
            + "cp.view DESC, cp.id DESC ";

    @Query(value = "SELECT cp.* FROM community_puzzle cp WHERE " + TREND_FILTER
            + "AND cp.created_at > :since " + TREND_ORDER + "LIMIT :size", nativeQuery = true)
    List<CommunityPuzzle> findTrendPuzzlesSince(@Param("since") Instant since,
                                               @Param("now") Instant now,
                                               @Param("halfLifeSeconds") long halfLifeSeconds,
                                               @Param("size") int size);

    // For a quiet week: the latest 30 older candidates, ranked the same way
    @Query(value = "SELECT cp.* FROM (SELECT * FROM community_puzzle cp WHERE " + TREND_FILTER
            + "AND cp.created_at <= :since ORDER BY cp.created_at DESC LIMIT 30) cp "
            + TREND_ORDER + "LIMIT :size", nativeQuery = true)
    List<CommunityPuzzle> findOlderTrendPuzzles(@Param("since") Instant since,
                                               @Param("now") Instant now,
                                               @Param("halfLifeSeconds") long halfLifeSeconds,
                                               @Param("size") int size);

    @Query(value = "SELECT COUNT(*) FROM community_puzzle " +
            "WHERE author_id = :userId AND created_at > :since", nativeQuery = true)
    long countByAuthorSinceIncludingDeleted(@Param("userId") Long userId, @Param("since") Instant since);

    // Puzzle count and vote totals for every ranked author in one query; deleted puzzles don't count
    @Query("SELECT p.user.id AS userId, COUNT(p) AS puzzleCount, " +
            "COALESCE(SUM(p.likeCount), 0) AS likeSum, COALESCE(SUM(p.dislikeCount), 0) AS dislikeSum " +
            "FROM CommunityPuzzle p WHERE p.user.id IN :userIds GROUP BY p.user.id")
    List<AuthorStatsProjection> sumAuthorStatsByUserIds(@Param("userIds") Collection<Long> userIds);

    @Query("SELECT DISTINCT p.user FROM CommunityPuzzle p WHERE p.createdAt >= :since")
    List<UserEntity> findUsersWhoCreatedPuzzlesSince(@Param("since") Instant since);

}
