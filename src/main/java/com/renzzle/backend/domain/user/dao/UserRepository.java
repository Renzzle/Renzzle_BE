package com.renzzle.backend.domain.user.dao;

import com.renzzle.backend.domain.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<UserEntity> findByEmail(String email);

}
