package com.renzzle.backend.domain.puzzle.dao;

import com.renzzle.backend.domain.puzzle.domain.UserCommunityPuzzle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserCommunityPuzzleRepository extends JpaRepository<UserCommunityPuzzle, Long> {

    @Query("SELECT u FROM UserCommunityPuzzle u WHERE u.user.id = :userId AND u.puzzle.id = :puzzleId")
    Optional<UserCommunityPuzzle> findUserPuzzleInfo(@Param("userId") Long userId, @Param("puzzleId") Long puzzleId);

}
