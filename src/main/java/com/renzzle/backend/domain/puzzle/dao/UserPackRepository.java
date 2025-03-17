package com.renzzle.backend.domain.puzzle.dao;

import com.renzzle.backend.domain.puzzle.domain.UserPack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPackRepository extends JpaRepository<UserPack, Long> {

    List<UserPack> findAllByUserIdAndPackIdIn(Long userId, List<Long> packIds);
}
