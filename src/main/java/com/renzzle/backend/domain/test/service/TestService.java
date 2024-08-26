package com.renzzle.backend.domain.test.service;

import com.renzzle.backend.domain.test.api.request.SaveEntityRequest;
import com.renzzle.backend.domain.test.api.response.HelloResponse;
import com.renzzle.backend.domain.test.dao.TestRepository;
import com.renzzle.backend.domain.test.domain.TestEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;

    public HelloResponse getHelloResponse(String name) {
        return new HelloResponse("Hello " + name + "!");
    }

    public TestEntity saveEntity(TestEntity entity) {
        return testRepository.save(entity);
    }

}
