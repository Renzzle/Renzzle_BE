package com.renzzle.backend.global.init;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        String statusSql = "INSERT INTO status (name) VALUES ('CREATED'), ('DELETED')";
        jdbcTemplate.execute(statusSql);

        String userLevelSql = "INSERT INTO user_level (name) VALUES ('BEGINNER'), ('INTERMEDIATE'), ('ADVANCED')";
        jdbcTemplate.execute(userLevelSql);

        String colorSql = "INSERT INTO color (name) VALUES ('RED'), ('ORANGE'), ('GREEN'), ('BLUE'), ('INDIGO'), ('PURPLE'), ('DARK_RED'), ('DARK_ORANGE'), ('DARK_GREEN'), ('DARK_BLUE'), ('DARK_INDIGO'), ('DARK_PURPLE')";
        jdbcTemplate.execute(colorSql);
    }

}