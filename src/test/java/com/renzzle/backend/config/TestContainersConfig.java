package com.renzzle.backend.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

public class TestContainersConfig implements ApplicationContextInitializer<ConfigurableApplicationContext>{

    private static final MySQLContainer<?> mysqlContainer =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    private static final GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:6.2"))
                    .withExposedPorts(6379)
                    .withEnv("REDIS_PASSWORD", "715095");

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
        try {
            mysqlContainer.start();
            redisContainer.start();

            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                    "spring.datasource.url=" + mysqlContainer.getJdbcUrl(),
                    "spring.datasource.username=" + mysqlContainer.getUsername(),
                    "spring.datasource.password=" + mysqlContainer.getPassword(),
                    "spring.data.redis.host=" + redisContainer.getHost(),
                    "spring.data.redis.port=" + redisContainer.getFirstMappedPort(),
                    "REDIS_PASSWORD=" + "715095"
            );
        } catch (Exception e) {
            throw new RuntimeException("TestContainers Failed: " + e.getMessage(), e);
        }
    }

}

