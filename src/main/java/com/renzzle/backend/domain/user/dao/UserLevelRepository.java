package com.renzzle.backend.domain.user.dao;

import com.renzzle.backend.domain.user.domain.UserLevel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLevelRepository extends JpaRepository<UserLevel, String> {
    }
