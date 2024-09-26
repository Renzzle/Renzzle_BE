package com.renzzle.backend.global.init;

import com.renzzle.backend.domain.puzzle.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.domain.WinColor;
import com.renzzle.backend.domain.user.domain.Color;
import com.renzzle.backend.domain.user.domain.UserLevel;
import com.renzzle.backend.global.common.domain.Status;
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
                    getInsertEnumSql("status", Status.StatusName.class),
                    getInsertEnumSql("user_level", UserLevel.LevelName.class),
                    getInsertEnumSql("color", Color.ColorName.class),
                    getInsertEnumSql("difficulty", Difficulty.DifficultyName.class),
                    getInsertEnumSql("win_color", WinColor.WinColorName.class)
            );
        } catch(DuplicateKeyException e) {
            log.warn("Data already exists");
        }
    }

    private <E extends Enum<E>> String getInsertEnumSql(String tableName, Class<E> enumClass) {
        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ");
        sqlBuilder.append(tableName).append(" (name) VALUES ");

        E[] enumConstants = enumClass.getEnumConstants();
        for (int i = 0; i < enumConstants.length; i++) {
            sqlBuilder.append("('").append(enumConstants[i].name()).append("')");
            if (i < enumConstants.length - 1) {
                sqlBuilder.append(", ");
            }
        }

        return sqlBuilder.toString();
    }

}
