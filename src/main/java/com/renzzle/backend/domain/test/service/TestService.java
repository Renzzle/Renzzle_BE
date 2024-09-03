package com.renzzle.backend.domain.test.service;

import com.renzzle.backend.domain.test.api.response.HelloResponse;
import com.renzzle.backend.domain.test.dao.JdbcEntityDao;
import com.renzzle.backend.domain.test.dao.TestRepository;
import com.renzzle.backend.domain.test.domain.JdbcEntity;
import com.renzzle.backend.domain.test.domain.TestEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final JdbcEntityDao jdbcEntityDao;

    public HelloResponse getHelloResponse(String name) {
        return new HelloResponse("Hello " + name + "!");
    }

    public TestEntity saveEntity(TestEntity entity) {
        return testRepository.save(entity);
    }

    public TestEntity findEntityById(Long id) {
        Optional<TestEntity> result = testRepository.findById(id);
        if(result.isEmpty())
            throw new CustomException(ErrorCode.GLOBAL_NOT_FOUND);
        return result.orElse(null);
    }

    public long saveJdbcEntity(String name) {
        return jdbcEntityDao.save(name);
    }

    public JdbcEntity findJdbcEntityById(Long id) {
        return jdbcEntityDao.findById(id);
    }

}
