package com.renzzle.backend.domain.puzzle.training.dao;

import com.renzzle.backend.domain.puzzle.training.domain.UserPack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPackRepository extends JpaRepository<UserPack, Long> {

    List<UserPack> findAllByUserIdAndPackIdIn(Long userId, List<Long> packIds);

    Optional<UserPack> findByUserIdAndPackId(Long userId, Long id);
}
