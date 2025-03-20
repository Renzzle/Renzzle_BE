package com.renzzle.backend.domain.auth.api;

import com.renzzle.backend.config.TestContainersConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@WebMvcTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
public class AuthControllerTest {



}
