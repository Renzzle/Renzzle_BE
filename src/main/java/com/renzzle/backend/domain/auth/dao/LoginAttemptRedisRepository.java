package com.renzzle.backend.domain.auth.dao;

import com.renzzle.backend.domain.auth.domain.LoginAttemptEntity;
import org.springframework.data.repository.CrudRepository;

public interface LoginAttemptRedisRepository extends CrudRepository<LoginAttemptEntity, String> {
}
