package com.renzzle.backend.domain.user.dao;

import com.renzzle.backend.domain.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

}
