package com.renzzle.backend.domain.puzzle.community.dao;

import com.renzzle.backend.domain.puzzle.community.dao.projection.LikeDislikeProjection;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.community.domain.UserCommunityPuzzle;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.support.DataJpaTestWithInitContainers;
import com.renzzle.backend.support.TestCommunityPuzzleBuilder;
import com.renzzle.backend.support.TestUserCommunityPuzzleBuilder;
import com.renzzle.backend.support.TestUserEntityBuilder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithInitContainers
public class UserCommunityPuzzleRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityPuzzleRepository communityPuzzleRepository;

    @Autowired
    private UserCommunityPuzzleRepository userCommunityPuzzleRepository;

    @Test
    public void findByUserIdAndPuzzleId_WhenExists_ThenReturnRecord() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);

        UserCommunityPuzzle ucp = TestUserCommunityPuzzleBuilder.builder(user, puzzle).save(userCommunityPuzzleRepository);

        // When
        Optional<UserCommunityPuzzle> result = userCommunityPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), puzzle.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(ucp.getId());
    }

    @Test
    public void checkIsSolvedPuzzle_WhenSolved_ThenReturnsTrue() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);

        TestUserCommunityPuzzleBuilder.builder(user, puzzle)
                .withSolved(true)
                .save(userCommunityPuzzleRepository);

        // When
        boolean result = userCommunityPuzzleRepository.checkIsSolvedPuzzle(user.getId(), puzzle.getId());

        // Then
        assertThat(result).isTrue();
    }

    @Test
    public void getMyLikeDislike_WhenLiked_ThenReturnTrueFalseProjection() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);

        TestUserCommunityPuzzleBuilder.builder(user, puzzle)
                .withLiked(true)
                .save(userCommunityPuzzleRepository);

        // When
        Optional<LikeDislikeProjection> projection = userCommunityPuzzleRepository.getMyLikeDislike(user.getId(), puzzle.getId());

        // Then
        assertThat(projection).isPresent();
        assertThat(projection.get().getIsLiked()).isTrue();
        assertThat(projection.get().getIsDisliked()).isFalse();
    }

    @Test
    public void solvePuzzle_WhenExecuted_ThenIsSolvedBecomesTrue() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        TestUserCommunityPuzzleBuilder.builder(user, puzzle).save(userCommunityPuzzleRepository);
        Instant fixedTime = Instant.parse("2025-04-15T12:00:00.000000Z");

        // When
        int updatedCount = userCommunityPuzzleRepository.solvePuzzle(user.getId(), puzzle.getId(), fixedTime);
        entityManager.flush();
        entityManager.clear();

        // Then
        UserCommunityPuzzle updated = userCommunityPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), puzzle.getId()).orElseThrow();

        assertThat(updatedCount).isEqualTo(1);
        assertThat(updated.isSolved()).isTrue();
        assertThat(updated.getSolvedAt().truncatedTo(ChronoUnit.MICROS))
                .isEqualTo(fixedTime.truncatedTo(ChronoUnit.MICROS));
    }

}
