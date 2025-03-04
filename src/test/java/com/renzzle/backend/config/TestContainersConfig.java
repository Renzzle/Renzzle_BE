package com.renzzle.backend.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

//@Testcontainers
public class TestContainersConfig implements ApplicationContextInitializer<ConfigurableApplicationContext>{

    // MySQL 컨테이너
    protected static MySQLContainer<?> mysqlContainer =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    // Redis 컨테이너
    protected static GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:6.2"))
                    .withExposedPorts(6379);

    static {
        mysqlContainer.start();
        redisContainer.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                "spring.datasource.url=" + mysqlContainer.getJdbcUrl(),
                "spring.datasource.username=" + mysqlContainer.getUsername(),
                "spring.datasource.password=" + mysqlContainer.getPassword(),
                "spring.data.redis.host=" + redisContainer.getHost(),
                "spring.data.redis.port=" + redisContainer.getFirstMappedPort(),
                // 만약 @Value("${REDIS_PASSWORD}")를 사용한다면 key는 "REDIS_PASSWORD"로 설정
                "REDIS_PASSWORD=715095"
        );
    }
}

