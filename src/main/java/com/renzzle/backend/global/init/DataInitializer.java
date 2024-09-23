package com.renzzle.backend.global.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.batchUpdate(
                    "INSERT INTO status (name) VALUES ('CREATED'), ('DELETED')",
                    "INSERT INTO user_level (name) VALUES ('BEGINNER'), ('INTERMEDIATE'), ('ADVANCED')",
                    "INSERT INTO color (name) VALUES ('RED'), ('ORANGE'), ('GREEN'), ('BLUE'), ('INDIGO'), ('PURPLE'), ('DARK_RED'), ('DARK_ORANGE'), ('DARK_GREEN'), ('DARK_BLUE'), ('DARK_INDIGO'), ('DARK_PURPLE')",
                    "INSERT INTO difficulty (name) VALUES ('HIGH'), ('MIDDLE'), ('LOW')",
                    "INSERT INTO win_color (name) VALUES ('BLACK'), ('WHITE')"
            );
        } catch(DuplicateKeyException e) {
            log.warn("Data already exists");
        }
    }

}
