package com.renzzle.backend.domain.test.service;

import com.renzzle.backend.domain.test.api.response.HelloResponse;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    public HelloResponse getHelloResponse(String name) {
        return new HelloResponse("Hello " + name + "!");
    }

}
