package com.renzzle.backend.domain.user.dao;

import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.user.domain.Title;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.support.DataJpaTestWithInitContainers;
import com.renzzle.backend.support.TestCommunityPuzzleBuilder;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithInitContainers
public class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    CommunityPuzzleRepository communityPuzzleRepository;

    @Test
    void existsByEmail_WhenEmailExists_ThenReturnTrue() {
        String email = "emailCheck@test.com";
        TestUserEntityBuilder.builder()
                .withEmail(email)
                .save(userRepository);

        assertThat(userRepository.existsByEmail(email)).isTrue();
    }

    @Test
    void findByEmail_WhenUserExists_ThenReturnUser() {
        String email = "findUser@test.com";
        String nickname = "findUser";
        TestUserEntityBuilder.builder()
                .withNickname(nickname)
                .withEmail(email)
                .save(userRepository);

        Optional<UserEntity> found = userRepository.findByEmail(email);
        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo(nickname);
    }

    @Test
    void existsByNickname_WhenNicknameExists_ThenReturnTrue() {
        String nickname = "nickCheck";
        TestUserEntityBuilder.builder()
                .withNickname(nickname)
                .save(userRepository);

        assertThat(userRepository.existsByNickname(nickname)).isTrue();
    }

    @Test
    void existsByDeviceId_WhenDeviceIdExists_ThenReturnTrue() {
        String deviceId = "device-id-123";
        TestUserEntityBuilder.builder()
                .withDeviceId(deviceId)
                .save(userRepository);

        assertThat(userRepository.existsByDeviceId(deviceId)).isTrue();
    }

    @Test
    void softDelete_WhenCalled_ThenSetStatusToDeletedAndSetDeletedAt() {
        var user = TestUserEntityBuilder.builder().save(userRepository);
        Instant deletedAt = Instant.parse("2025-04-15T12:00:00.000000Z");

        userRepository.softDelete(user.getId(), deletedAt);
        UserEntity deletedUser = userRepository.findByIdIncludingDeleted(user.getId());

        assertThat(deletedUser.getStatus().getName()).isEqualTo(Status.StatusName.DELETED.name());
        assertThat(deletedUser.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    void addUserCurrency_WhenCalled_ThenCurrencyIsIncreased() {
        var user = TestUserEntityBuilder.builder().withCurrency(100).save(userRepository);

        userRepository.addUserCurrency(user.getId(), 50);
        UserEntity updatedUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(updatedUser.getCurrency()).isEqualTo(150);
    }

    @Test
    void isLastAccessBeforeToday_WhenLastAccessedYesterday_ThenReturnTrue() {
        Instant yesterday = Instant.now().minusSeconds(86400);
        var user = TestUserEntityBuilder.builder().withLastAccessedAt(yesterday).save(userRepository);

        Boolean result = userRepository.isLastAccessBeforeToday(user.getId());

        assertThat(result).isTrue();
    }

    @Test
    void updateLastAccessedAt_WhenCalled_ThenUpdatedCorrectly() {
        var user = TestUserEntityBuilder.builder().save(userRepository);
        Instant now = Instant.parse("2025-04-15T12:00:00.000000Z");

        userRepository.updateLastAccessedAt(user.getId(), now);
        UserEntity updatedUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(updatedUser.getLastAccessedAt()).isEqualTo(now);
    }

    @Test
    void getUserTitle_WhenTitleExists_ThenReturnTitle() {
        var user = TestUserEntityBuilder.builder().save(userRepository);

        Optional<Title> title = userRepository.getUserTitle(user.getId());

        assertThat(title).isPresent();
        assertThat(title.get()).isEqualTo(user.getTitle());
    }

    @Test
    void updateUserTitle_WhenCalled_ThenTitleIsUpdated() {
        var user = TestUserEntityBuilder.builder().save(userRepository);
        Title newTitle = Title.getTitle(Title.TitleType.MASTER);

        userRepository.updateUserTitle(user.getId(), newTitle);
        UserEntity updated = userRepository.findById(user.getId()).orElseThrow();

        assertThat(updated.getTitle()).isEqualTo(newTitle);
    }

    @Test
    void isUserQualified_WhenAllConditionsMet_ThenReturnTrue() {
        var user = TestUserEntityBuilder.builder()
                .withRating(2000)
                .save(userRepository);

        new TestCommunityPuzzleBuilder(user)
                .withLikeCount(50)
                .withSolvedCount(20)
                .save(communityPuzzleRepository);

        new TestCommunityPuzzleBuilder(user)
                .withLikeCount(20)
                .withSolvedCount(10)
                .save(communityPuzzleRepository);

        int minLikes = 70;
        int minPuzzleCount = 2;
        double minRating = 2000;
        int minSolverCount = 30;

        boolean result = userRepository.isUserQualified(
                user.getId(),
                minLikes,
                minPuzzleCount,
                minRating,
                minSolverCount
        );

        assertThat(result).isTrue();
    }

    @Test
    void isUserQualified_WhenAnyConditionFails_ThenReturnFalse() {
        var user = TestUserEntityBuilder.builder()
                .withRating(1000)
                .save(userRepository);

        new TestCommunityPuzzleBuilder(user)
                .withLikeCount(10)
                .withSolvedCount(5)
                .save(communityPuzzleRepository);

        int minLikes = 50;
        int minPuzzleCount = 1;
        double minRating = 1000;
        int minSolverCount = 0;

        boolean result = userRepository.isUserQualified(
                user.getId(),
                minLikes,
                minPuzzleCount,
                minRating,
                minSolverCount
        );

        assertThat(result).isFalse();
    }

}
