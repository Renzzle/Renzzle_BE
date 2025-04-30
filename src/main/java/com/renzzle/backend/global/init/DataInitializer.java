package com.renzzle.backend.global.init;

import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.domain.Admin;
import com.renzzle.backend.domain.puzzle.training.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.Title;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.LangCode;
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
        initializeDefaultValue();
        addAdminAccount();
        initializeSystemInfo();
    }

    private void initializeSystemInfo() {
        try {
            jdbcTemplate.batchUpdate("INSERT IGNORE INTO system_info (id, version, system_check)\n" +
                    "VALUES (1, '1.0.0', false);");
        } catch (DuplicateKeyException e) {
            log.warn("System info already exists");
        }
    }

    private void addAdminAccount() {
        try {
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
        } catch (DuplicateKeyException e) {
            log.warn("Admin already exists");
        }
    }

    private void initializeDefaultValue() {
        tryInsertEnumSql("status", Status.StatusName.class);
        tryInsertEnumSql("difficulty", Difficulty.DifficultyName.class);
        tryInsertEnumSql("win_color", WinColor.WinColorName.class);
        tryInsertEnumSql("lang_code", LangCode.LangCodeName.class);
        tryInsertEnumSql("title", Title.TitleType.class);
    }

    private <E extends Enum<E>> void tryInsertEnumSql(String tableName, Class<E> enumClass) {
        StringBuilder sqlBuilder = new StringBuilder("INSERT IGNORE INTO ");
        sqlBuilder.append(tableName).append(" (name) VALUES ");

        E[] enumConstants = enumClass.getEnumConstants();
        for (int i = 0; i < enumConstants.length; i++) {
            sqlBuilder.append("('").append(enumConstants[i].name()).append("')");
            if (i < enumConstants.length - 1) {
                sqlBuilder.append(", ");
            }
        }

        try {
            jdbcTemplate.batchUpdate(sqlBuilder.toString());
        } catch (DuplicateKeyException e) {
            log.warn("{} already exists", tableName);
        }
    }

}
