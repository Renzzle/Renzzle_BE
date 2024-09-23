package com.renzzle.backend.domain.puzzle.dao;

import com.renzzle.backend.domain.puzzle.domain.UserCommunityPuzzle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCommunityPuzzleRepository extends JpaRepository<UserCommunityPuzzle, Long> {
}
