package com.renzzle.backend.domain.auth.dao;

import com.renzzle.backend.config.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
class RefreshSessionRepositoryTest {

    private static final long USER_ID = 900_001L;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    private final String phone = UUID.randomUUID().toString();
    private final String tablet = UUID.randomUUID().toString();

    @AfterEach
    void tearDown() {
        refreshSessionRepository.deleteAll(USER_ID);
    }

    @Test
    void save_WhenSessionSavedAgain_ThenOnlyTheLatestTokenRemains() {
        refreshSessionRepository.save(USER_ID, phone, "first");
        refreshSessionRepository.save(USER_ID, phone, "second");

        assertThat(refreshSessionRepository.findToken(phone)).contains("second");
    }

    @Test
    void delete_WhenOneDeviceLogsOut_ThenTheOtherStaysLoggedIn() {
        refreshSessionRepository.save(USER_ID, phone, "phone-token");
        refreshSessionRepository.save(USER_ID, tablet, "tablet-token");

        refreshSessionRepository.delete(USER_ID, phone);

        assertThat(refreshSessionRepository.findToken(phone)).isEmpty();
        assertThat(refreshSessionRepository.findToken(tablet)).contains("tablet-token");
    }

    @Test
    void deleteAllExcept_WhenCurrentSessionKept_ThenOnlyItRemains() {
        refreshSessionRepository.save(USER_ID, phone, "phone-token");
        refreshSessionRepository.save(USER_ID, tablet, "tablet-token");

        refreshSessionRepository.deleteAllExcept(USER_ID, phone);

        assertThat(refreshSessionRepository.findToken(phone)).contains("phone-token");
        assertThat(refreshSessionRepository.findToken(tablet)).isEmpty();
    }

    @Test
    void deleteAll_WhenCalled_ThenEverySessionIsGone() {
        refreshSessionRepository.save(USER_ID, phone, "phone-token");
        refreshSessionRepository.save(USER_ID, tablet, "tablet-token");

        refreshSessionRepository.deleteAll(USER_ID);

        assertThat(refreshSessionRepository.findToken(phone)).isEmpty();
        assertThat(refreshSessionRepository.findToken(tablet)).isEmpty();
    }

}
