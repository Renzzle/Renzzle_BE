package com.renzzle.backend.global.init;

import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.domain.Admin;
import com.renzzle.backend.domain.puzzle.training.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import static com.renzzle.backend.domain.auth.domain.Admin.ADMIN;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.mail.username}")
    private String adminEmail;
    @Value("${spring.mail.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        try {
            initializeDefaultValue();
            addAdminAccount();
        } catch(DuplicateKeyException e) {
            log.warn("Data already exists");
        }
    }

    private void addAdminAccount() {
        if(!userRepository.existsByNickname(ADMIN)) {
            String adminDeviceId = "admin_device127";
            String encodedPassword = new BCryptPasswordEncoder().encode(adminPassword);
            UserEntity user = UserEntity.builder()
                    .email(adminEmail)
                    .password(encodedPassword)
                    .nickname(ADMIN)
                    .deviceId(adminDeviceId)
                    .build();
            UserEntity admin = userRepository.save(user);
            adminRepository.save(Admin.builder().user(admin).build());
        }
    }

    private void initializeDefaultValue() {
        jdbcTemplate.batchUpdate(
                getInsertEnumSql("status", Status.StatusName.class),
                getInsertEnumSql("difficulty", Difficulty.DifficultyName.class),
                getInsertEnumSql("win_color", WinColor.WinColorName.class)
        );
    }

    private <E extends Enum<E>> String getInsertEnumSql(String tableName, Class<E> enumClass) {
        StringBuilder sqlBuilder = new StringBuilder("INSERT IGNORE INTO ");
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
