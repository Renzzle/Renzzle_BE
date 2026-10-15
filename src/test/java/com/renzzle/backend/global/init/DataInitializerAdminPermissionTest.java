package com.renzzle.backend.global.init;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.domain.Admin;
import com.renzzle.backend.domain.auth.domain.AdminPermission;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
class DataInitializerAdminPermissionTest {

    @Autowired private DataInitializer dataInitializer;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UserEntity adminUser;
    private Admin admin;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(
                TestUserFactory.createTestUser("legacy-" + UUID.randomUUID().toString().substring(0, 8), 1000));
        admin = adminRepository.save(Admin.builder().user(adminUser).build());
    }

    @AfterEach
    void tearDown() {
        userRepository.delete(adminUser);
    }

    @Test
    void run_WhenAdminWasSavedBeforePermissionsExisted_ThenGivesItEveryPermission() {
        // What ddl-auto leaves in rows that existed before the column
        jdbcTemplate.update("UPDATE admin SET permissions = NULL WHERE id = ?", admin.getId());

        dataInitializer.run();

        assertThat(adminRepository.findByUser(adminUser).orElseThrow().getPermissions())
                .containsExactlyInAnyOrder(AdminPermission.values());
    }

    @Test
    void run_WhenAdminWasLeftWithoutPermissions_ThenKeepsItThatWay() {
        dataInitializer.run();

        assertThat(adminRepository.findByUser(adminUser).orElseThrow().getPermissions()).isEmpty();
    }
}
