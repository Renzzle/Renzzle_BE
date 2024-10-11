package com.renzzle.backend.domain.auth.dao;

import com.renzzle.backend.domain.auth.domain.Admin;
import com.renzzle.backend.domain.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    boolean existsByUser(UserEntity user);

}
