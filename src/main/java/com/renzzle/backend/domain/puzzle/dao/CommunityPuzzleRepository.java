package com.renzzle.backend.domain.puzzle.dao;

import com.renzzle.backend.domain.puzzle.domain.CommunityPuzzle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface CommunityPuzzleRepository extends JpaRepository<CommunityPuzzle, Long> {

    @Query(value =
            "SELECT * FROM community_puzzle cp " +
            "WHERE (cp.created_at < :lastCreatedAt) OR (cp.created_at = :lastCreatedAt AND cp.id > :lastId) " +
            "ORDER BY cp.created_at DESC, cp.id ASC " +
            "LIMIT :size"
            , nativeQuery = true)
    List<CommunityPuzzle> findPuzzlesSortByCreatedAt(
            @Param("lastCreatedAt") Instant lastCreatedAt,
            @Param("lastId") long lastId,
            @Param("size") int size
    );

    @Query(value =
            "SELECT * FROM community_puzzle cp " +
            "WHERE (cp.like_count < :lastLikeCnt) OR (cp.like_count = :lastLikeCnt AND cp.id > :lastId) " +
            "ORDER BY cp.like_count DESC, cp.id ASC " +
            "LIMIT :size"
            , nativeQuery = true)
    List<CommunityPuzzle> findPuzzlesSortByLike(
            @Param("lastLikeCnt") int lastLikeCnt,
            @Param("lastId") long lastId,
            @Param("size") int size
    );

    List<CommunityPuzzle> findByTitleContaining(String title);

    @Query(value = "SELECT cp.* FROM community_puzzle cp " +
            "JOIN user u ON cp.author_id = u.id " +
            "WHERE u.nickname LIKE CONCAT('%', :name, '%')",
            nativeQuery = true)
    List<CommunityPuzzle> findByAuthorName(@Param("name") String name);

}
