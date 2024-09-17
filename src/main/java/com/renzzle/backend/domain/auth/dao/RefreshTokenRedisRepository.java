package com.renzzle.backend.domain.auth.dao;

import com.renzzle.backend.domain.auth.domain.RefreshTokenEntity;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRedisRepository extends CrudRepository<RefreshTokenEntity, Long> {
}
