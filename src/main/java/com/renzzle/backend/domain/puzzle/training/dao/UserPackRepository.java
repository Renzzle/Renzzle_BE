package com.renzzle.backend.domain.puzzle.training.dao;

import com.renzzle.backend.domain.puzzle.training.domain.UserPack;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserPackRepository extends JpaRepository<UserPack, Long> {

    List<UserPack> findAllByUserIdAndPackIdIn(Long userId, List<Long> packIds);

    Optional<UserPack> findByUserIdAndPackId(Long userId, Long id);

    @Modifying
    @Transactional
    @Query("UPDATE UserPack u SET u.solvedCount = u.solvedCount + 1 WHERE u.user.id = :userId AND u.pack.id = :packId")
    void increaseSolvedCount(@Param("userId") Long userId, @Param("packId") Long packId);

    @Modifying
    @Transactional
    @Query("UPDATE UserPack u SET u.solvedCount = u.solvedCount - 1 " +
            "WHERE u.user.id = :userId AND u.pack.id = :packId AND u.solvedCount > 0")
    void decreaseSolvedCount(@Param("userId") Long userId, @Param("packId") Long packId);
}
