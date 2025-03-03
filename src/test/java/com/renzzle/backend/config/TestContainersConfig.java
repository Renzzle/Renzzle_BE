package com.renzzle.backend.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestContainersConfig {

    private static final String MYSQL_DOCKER_IMAGE  = "mysql:8.0";
    private static final String REDIS_DOCKER_IMAGE = "redis:6.2";

    @Container
    public static MySQLContainer<?> mysqlContainer = new MySQLContainer<>(MYSQL_DOCKER_IMAGE)
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
//            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withReuse(true);
//            .withDatabaseName("testdb")
//            .withUsername("testuser")
//            .withPassword("testpass");

    @Container
    public static GenericContainer<?> redisContainer = new GenericContainer<>(REDIS_DOCKER_IMAGE)
            .withExposedPorts(6379);
//            .withEnv("REDIS_PASSWORD", "715095");

    static {
        mysqlContainer.start();
        redisContainer.start();
    }
}

//    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
//        @Override
//        public void initialize(ConfigurableApplicationContext applicationContext) {
//            TestPropertyValues.of(
//                    "spring.datasource.url=" + mysqlContainer.getJdbcUrl(),
//                    "spring.datasource.username=" + mysqlContainer.getUsername(),
//                    "spring.datasource.password=" + mysqlContainer.getPassword(),
//
//                    "spring.redis.host=" + redisContainer.getHost(),
//                    "spring.redis.port=" + redisContainer.getMappedPort(6379)
//            ).applyTo(applicationContext.getEnvironment());
//        }
//    }


//    @DynamicPropertySource
//    static void configureProperties(DynamicPropertyRegistry registry) {
//        // MySQL 설정
//        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
//        registry.add("spring.datasource.username", mysqlContainer::getUsername);
//        registry.add("spring.datasource.password", mysqlContainer::getPassword);
////        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
//
////        // JPA 설정
////        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQL8Dialect");
////        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update"); // or create-drop
////        registry.add("spring.jpa.show-sql", () -> "true");
////
////        // Redis 설정
////        registry.add("spring.data.redis.host", redisContainer::getHost);
////        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
////        registry.add("spring.data.redis.password", () -> "715095");
////
////        registry.add("jwt.secret", () -> "a8fd6gh2389asfv73ruh3f91h13jhfdf7913f9sda9svbmwpeghb327r3bg1g1vs03052gisafhv9238bfoigu04100409ad0nbmqpa74kf72os94iwkk2900fosizzvvi3hsdgji");
////        registry.add("spring.mail.username", () -> "renzzle.official@gmail.com");
////        registry.add("spring.mail.password", () -> "xistxvwwsylmztnp");
//    }

