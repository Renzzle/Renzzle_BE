package com.renzzle.backend.domain.auth.dao;

import com.renzzle.backend.domain.auth.domain.AuthEmailEntity;
import org.springframework.data.repository.CrudRepository;

public interface EmailRedisRepository extends CrudRepository<AuthEmailEntity, String> {
}
