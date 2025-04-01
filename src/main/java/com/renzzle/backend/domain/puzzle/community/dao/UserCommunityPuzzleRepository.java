package com.renzzle.backend.domain.puzzle.community.dao;

import com.renzzle.backend.domain.puzzle.community.domain.UserCommunityPuzzle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCommunityPuzzleRepository extends JpaRepository<UserCommunityPuzzle, Long> {

    @Query("SELECT CASE WHEN EXISTS (" +
            "    SELECT 1 FROM UserCommunityPuzzle ucp " +
            "    WHERE ucp.user.id = :userId " +
            "    AND ucp.puzzle.id = :puzzleId " +
            "    AND ucp.isSolved = TRUE" +
            ") THEN TRUE ELSE FALSE END")
    boolean checkIsSolvedPuzzle(@Param("userId") Long userId, @Param("puzzleId") Long puzzleId);

    @Query("SELECT ucp.like " +
            "FROM UserCommunityPuzzle ucp " +
            "WHERE ucp.user.id = :userId " +
            "AND ucp.puzzle.id = :puzzleId")
    Optional<Boolean> getMyLike(@Param("userId") Long userId, @Param("puzzleId") Long puzzleId);

    @Query("SELECT ucp.dislike " +
            "FROM UserCommunityPuzzle ucp " +
            "WHERE ucp.user.id = :userId " +
            "AND ucp.puzzle.id = :puzzleId")
    Optional<Boolean> getMyDislike(@Param("userId") Long userId, @Param("puzzleId") Long puzzleId);

    @Query("SELECT u FROM UserCommunityPuzzle u WHERE u.user.id = :userId AND u.puzzle.id = :puzzleId")
    Optional<UserCommunityPuzzle> findUserPuzzleInfo(@Param("userId") Long userId, @Param("puzzleId") Long puzzleId);

    @Query("SELECT u.puzzle.id FROM UserCommunityPuzzle u WHERE u.user.id = :userId")
    List<Long> findCommunityIdsByUserId(@Param("userId") Long userId);

//    @Query(value =
//            "SELECT cp FROM UserCommunityPuzzle ucp " +
//                    "JOIN ucp.puzzle cp " +
//                    "WHERE ucp.user.id = :userId AND " +
//                    "(cp.createdAt < :lastCreatedAt OR (cp.createdAt = :lastCreatedAt AND cp.id > :lastId)) " +
//                    "ORDER BY cp.createdAt DESC, cp.id ASC" +
//                    "LIMIT :size"
//                    , nativeQuery = true)
//    List<CommunityPuzzle> findUserPuzzlesSortByCreatedAt(
//            @Param("lastCreatedAt") Instant lastCreatedAt,
//            @Param("lastId") long lastId,
//            @Param("size") int size,
//            @Param("userId") Long userId);

}
