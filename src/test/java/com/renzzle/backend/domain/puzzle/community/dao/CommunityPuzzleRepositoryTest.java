package com.renzzle.backend.domain.puzzle.community.dao;

import com.renzzle.backend.config.DataJpaTestWithInitContainers;
import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.support.TestCommunityPuzzleBuilder;
import com.renzzle.backend.support.TestUserCommunityPuzzleBuilder;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.renzzle.backend.domain.puzzle.shared.domain.WinColor.getWinColor;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithInitContainers
public class CommunityPuzzleRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityPuzzleRepository communityPuzzleRepository;

    @Autowired
    private UserCommunityPuzzleRepository userCommunityPuzzleRepository;

    @Test
    public void searchCommunityPuzzles_WhenVariousConditions_ThenReturnsExpectedResults() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder()
                .withNickname("test")
                .save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user)
                .withColor(getWinColor("BLACK"))
                .withDepth(7)
                .withVerified(true)
                .save(communityPuzzleRepository);

        TestUserCommunityPuzzleBuilder.builder(user, puzzle)
                .withSolved(true)
                .save(userCommunityPuzzleRepository);

        // When
        List<CommunityPuzzle> result = communityPuzzleRepository.searchCommunityPuzzles(
                new GetCommunityPuzzleRequest(null, null, null, "BLACK", true, 6, 7, true, "test"),
                user.getId()
        );

        // Then
        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(p -> {
                    assertThat(p.getId()).isEqualTo(puzzle.getId());
                    assertThat(p.getUser().getNickname()).isEqualTo(user.getNickname());
                    assertThat(p.getWinColor()).isEqualTo(getWinColor("BLACK"));
                    assertThat(p.getDepth()).isEqualTo(7);
                    assertThat(p.getIsVerified()).isTrue();
                });
    }

    @Test
    public void searchCommunityPuzzles_WhenCursorIdAndSizeProvided_ThenReturnsCorrectSubset() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);

        CommunityPuzzle puzzle1 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        CommunityPuzzle puzzle2 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        CommunityPuzzle puzzle3 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);

        // When
        List<CommunityPuzzle> result = communityPuzzleRepository.searchCommunityPuzzles(
                new GetCommunityPuzzleRequest(puzzle3.getId(), 2, null, null, null, null, null, null, null),
                user.getId()
        );

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting("id")
                .containsExactly(puzzle2.getId(), puzzle1.getId());
    }

    @Test
    public void searchCommunityPuzzles_WhenSortIsLike_ThenOrderedByLikeCountDesc() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);

        CommunityPuzzle puzzleA = TestCommunityPuzzleBuilder.builder(user)
                .withLikeCount(1)
                .save(communityPuzzleRepository);
        CommunityPuzzle puzzleB = TestCommunityPuzzleBuilder.builder(user)
                .withLikeCount(2)
                .save(communityPuzzleRepository);
        CommunityPuzzle puzzleC = TestCommunityPuzzleBuilder.builder(user)
                .withLikeCount(3)
                .save(communityPuzzleRepository);

        // When
        List<CommunityPuzzle> result = communityPuzzleRepository.searchCommunityPuzzles(
                new GetCommunityPuzzleRequest(null, 3, "LIKE", null, null, null, null, null, null),
                user.getId()
        );

        // Then
        assertThat(result)
                .extracting("id")
                .containsExactly(puzzleC.getId(), puzzleB.getId(), puzzleA.getId());
    }

    @Test
    public void searchCommunityPuzzles_WhenSortIsLatest_ThenOrderedByCreatedAtDesc() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);

        CommunityPuzzle puzzleA = TestCommunityPuzzleBuilder.builder(user)
                .save(communityPuzzleRepository);
        CommunityPuzzle puzzleB = TestCommunityPuzzleBuilder.builder(user)
                .save(communityPuzzleRepository);
        CommunityPuzzle puzzleC = TestCommunityPuzzleBuilder.builder(user)
                .save(communityPuzzleRepository);

        // When
        List<CommunityPuzzle> result = communityPuzzleRepository.searchCommunityPuzzles(
                new GetCommunityPuzzleRequest(null, 3, "LATEST", null, null, null, null, null, null),
                user.getId()
        );

        // Then
        assertThat(result)
                .extracting("id")
                .containsExactly(puzzleC.getId(), puzzleB.getId(), puzzleA.getId());
    }

    @Test
    public void getUserLikedPuzzles_WhenCursorIsNull_ThenReturnsLikedPuzzlesSorted() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);

        CommunityPuzzle puzzle1 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        CommunityPuzzle puzzle2 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        CommunityPuzzle puzzle3 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);

        Instant now = Instant.now();

        TestUserCommunityPuzzleBuilder.builder(user, puzzle1)
                .withLiked(true)
                .withLikedAt(now.minusSeconds(30))
                .save(userCommunityPuzzleRepository);

        TestUserCommunityPuzzleBuilder.builder(user, puzzle2)
                .withLiked(true)
                .withLikedAt(now.minusSeconds(20))
                .save(userCommunityPuzzleRepository);

        TestUserCommunityPuzzleBuilder.builder(user, puzzle3)
                .withLiked(true)
                .withLikedAt(now.minusSeconds(10))
                .save(userCommunityPuzzleRepository);

        // When
        List<CommunityPuzzle> result = communityPuzzleRepository.getUserLikedPuzzles(user.getId(), null, 10);

        // Then
        assertThat(result)
                .hasSize(3)
                .extracting("id")
                .containsExactly(puzzle3.getId(), puzzle2.getId(), puzzle1.getId()); // 최신 순
    }

    @Test
    public void getUserLikedPuzzles_WhenCursorProvided_ThenReturnsRemainingPuzzles() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);

        CommunityPuzzle puzzle1 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        CommunityPuzzle puzzle2 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        CommunityPuzzle puzzle3 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);

        Instant now = Instant.now();

        TestUserCommunityPuzzleBuilder.builder(user, puzzle1)
                .withLiked(true)
                .withLikedAt(now.minusSeconds(30))
                .save(userCommunityPuzzleRepository);

        TestUserCommunityPuzzleBuilder.builder(user, puzzle2)
                .withLiked(true)
                .withLikedAt(now.minusSeconds(20))
                .save(userCommunityPuzzleRepository);

        TestUserCommunityPuzzleBuilder.builder(user, puzzle3)
                .withLiked(true)
                .withLikedAt(now.minusSeconds(10))
                .save(userCommunityPuzzleRepository);

        // When
        List<CommunityPuzzle> result = communityPuzzleRepository.getUserLikedPuzzles(user.getId(), puzzle3.getId(), 10);

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting("id")
                .containsExactly(puzzle2.getId(), puzzle1.getId());
    }

    @Test
    public void getUserPuzzles_WhenCursorIsNull_ThenReturnSortedList() {
        // Given
        UserEntity user1 = TestUserEntityBuilder.builder().save(userRepository);
        UserEntity user2 = TestUserEntityBuilder.builder().save(userRepository);

        TestCommunityPuzzleBuilder.builder(user1)
                .withBoardStatus("1")
                .save(communityPuzzleRepository);
        TestCommunityPuzzleBuilder.builder(user1)
                .withBoardStatus("2")
                .save(communityPuzzleRepository);
        TestCommunityPuzzleBuilder.builder(user1)
                .withBoardStatus("3")
                .save(communityPuzzleRepository);
        TestCommunityPuzzleBuilder.builder(user2)
                .withBoardStatus("4")
                .save(communityPuzzleRepository);

        // When
        List<CommunityPuzzle> result = communityPuzzleRepository.getUserPuzzles(user1.getId(), null, 10);

        // Then
        assertThat(result)
                .hasSize(3)
                .extracting("boardStatus")
                .containsExactly("3", "2", "1");
    }

    @Test
    public void getUserPuzzles_WhenCursorProvided_ThenReturnOnlyPreviousOnes() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);

        TestCommunityPuzzleBuilder.builder(user)
                .withBoardStatus("A")
                .save(communityPuzzleRepository);
        TestCommunityPuzzleBuilder.builder(user)
                .withBoardStatus("B")
                .save(communityPuzzleRepository);
        CommunityPuzzle p3 = TestCommunityPuzzleBuilder.builder(user)
                .withBoardStatus("C")
                .save(communityPuzzleRepository);

        // When
        List<CommunityPuzzle> result = communityPuzzleRepository.getUserPuzzles(user.getId(), p3.getId(), 10);

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting("boardStatus")
                .containsExactly("B", "A");
    }

    @Test
    public void softDelete_WhenCalled_ThenPuzzleIsMarkedDeleted() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user)
                .save(communityPuzzleRepository);

        Instant deletedTime = Instant.now();

        // When
        int updatedCount = communityPuzzleRepository.softDelete(puzzle.getId(), deletedTime);

        // Then
        CommunityPuzzle deletedPuzzle = communityPuzzleRepository.findByIdIncludingDeleted(puzzle.getId());

        assertThat(updatedCount).isEqualTo(1);
        assertThat(deletedPuzzle.getStatus().getName()).isEqualTo(Status.StatusName.DELETED.name());
        assertThat(deletedPuzzle.getDeletedAt().truncatedTo(ChronoUnit.MICROS))
                .isEqualTo(deletedTime.truncatedTo(ChronoUnit.MICROS));
    }

}
