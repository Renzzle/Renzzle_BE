package com.renzzle.backend.domain.puzzle.community.dao;

import com.renzzle.backend.support.DataJpaTestWithInitContainers;
import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.support.TestCommunityPuzzleBuilder;
import com.renzzle.backend.support.TestUserCommunityPuzzleBuilder;
import com.renzzle.backend.support.TestUserEntityBuilder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static com.renzzle.backend.domain.puzzle.shared.domain.WinColor.getWinColor;
import static com.renzzle.backend.support.TestTime.FIXED_INSTANT;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithInitContainers
class CommunityPuzzleRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityPuzzleRepository communityPuzzleRepository;

    @Autowired
    private UserCommunityPuzzleRepository userCommunityPuzzleRepository;

    @Test
    void searchCommunityPuzzles_WhenVariousConditions_ThenReturnsExpectedResults() {
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
                new GetCommunityPuzzleRequest(null, null, null, null, "BLACK", true, 6, 7, true, "test"),
                user,
                0
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
    void searchCommunityPuzzles_WhenCursorIdAndSizeProvided_ThenReturnsCorrectSubset() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);

        CommunityPuzzle puzzle1 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        CommunityPuzzle puzzle2 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        CommunityPuzzle puzzle3 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);

        // When
        List<CommunityPuzzle> result = communityPuzzleRepository.searchCommunityPuzzles(
                new GetCommunityPuzzleRequest(puzzle3.getId(), 2, null, null, null, null, null, null, null, null),
                user,
                0
        );

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting("id")
                .containsExactly(puzzle2.getId(), puzzle1.getId());
    }

    @Test
    void searchCommunityPuzzles_WhenSortIsLike_ThenOrderedByLikeCountDesc() {
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
                new GetCommunityPuzzleRequest(null, 3, "LIKE", null, null, null, null, null, null, null),
                user,
                0
        );

        // Then
        assertThat(result)
                .extracting("id")
                .containsExactly(puzzleC.getId(), puzzleB.getId(), puzzleA.getId());
    }

    @Test
    void searchCommunityPuzzles_WhenSortIsLatest_ThenOrderedByCreatedAtDesc() {
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
                new GetCommunityPuzzleRequest(null, 3, "LATEST", null, null, null, null, null, null, null),
                user,
                0
        );

        // Then
        assertThat(result)
                .extracting("id")
                .containsExactly(puzzleC.getId(), puzzleB.getId(), puzzleA.getId());
    }

    @Test
    void getUserLikedPuzzles_WhenCursorIsNull_ThenReturnsLikedPuzzlesSorted() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);

        CommunityPuzzle puzzle1 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        CommunityPuzzle puzzle2 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        CommunityPuzzle puzzle3 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);

        Instant now = FIXED_INSTANT;

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
                .containsExactly(puzzle3.getId(), puzzle2.getId(), puzzle1.getId()); // newest first
    }

    @Test
    void getUserLikedPuzzles_WhenCursorProvided_ThenReturnsRemainingPuzzles() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);

        CommunityPuzzle puzzle1 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        CommunityPuzzle puzzle2 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        CommunityPuzzle puzzle3 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);

        Instant now = FIXED_INSTANT;

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
    void getUserPuzzles_WhenCursorIsNull_ThenReturnSortedList() {
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
    void getUserPuzzles_WhenCursorProvided_ThenReturnOnlyPreviousOnes() {
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
    void softDelete_WhenCalled_ThenPuzzleIsMarkedDeleted() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user)
                .save(communityPuzzleRepository);

        Instant deletedTime = Instant.parse("2025-04-15T12:00:00.000000Z");

        // When
        int updatedCount = communityPuzzleRepository.softDelete(puzzle.getId(), deletedTime);
        entityManager.flush();
        entityManager.clear();

        // Then
        CommunityPuzzle deletedPuzzle = communityPuzzleRepository.findByIdIncludingDeleted(puzzle.getId());

        assertThat(updatedCount).isEqualTo(1);
        assertThat(deletedPuzzle.getStatus().getName()).isEqualTo(Status.StatusName.DELETED.name());
        assertThat(deletedPuzzle.getDeletedAt().truncatedTo(ChronoUnit.MICROS))
                .isEqualTo(deletedTime.truncatedTo(ChronoUnit.MICROS));
    }


    @Test
    void searchCommunityPuzzles_WhenSortIsRecommend_ThenSameSeedYieldsSameOrder() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().withMmr(1500.0).save(userRepository);
        for (int i = 0; i < 10; i++) {
            TestCommunityPuzzleBuilder.builder(user).withRating(1500.0).save(communityPuzzleRepository);
        }

        // When
        List<Long> first = recommendIds(user, 42, null, 10);
        List<Long> second = recommendIds(user, 42, null, 10);

        // Then: the shuffle comes from (id, seed), never stored, so it must reproduce
        assertThat(first).hasSize(10).isEqualTo(second);
    }

    @Test
    void searchCommunityPuzzles_WhenSortIsRecommend_ThenDifferentSeedsReshuffle() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().withMmr(1500.0).save(userRepository);
        for (int i = 0; i < 10; i++) {
            TestCommunityPuzzleBuilder.builder(user).withRating(1500.0).save(communityPuzzleRepository);
        }

        // When
        List<Long> withSeed1 = recommendIds(user, 1, null, 10);
        List<Long> withSeed2 = recommendIds(user, 2, null, 10);

        // Then: same puzzles, different order
        assertThat(withSeed1).containsExactlyInAnyOrderElementsOf(withSeed2).isNotEqualTo(withSeed2);
    }

    @Test
    void searchCommunityPuzzles_WhenSortIsRecommend_ThenPuzzlesNearUserMmrComeFirst() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().withMmr(1500.0).save(userRepository);

        List<Long> nearIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            nearIds.add(TestCommunityPuzzleBuilder.builder(user)
                    .withRating(1500.0)
                    .save(communityPuzzleRepository)
                    .getId());
        }
        for (int i = 0; i < 5; i++) {
            TestCommunityPuzzleBuilder.builder(user)
                    .withRating(3000.0)
                    .save(communityPuzzleRepository);
        }

        // near = |1500-1500| + [0,600) = [0,600), far = |3000-1500| + [0,600) = [1500,2100).
        // The ranges cannot overlap, so the near five win for every seed.
        assertThat(recommendIds(user, 7, null, 5)).containsExactlyInAnyOrderElementsOf(nearIds);
    }

    @Test
    void searchCommunityPuzzles_WhenSortIsRecommend_ThenCursorPagingVisitsEachPuzzleOnce() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().withMmr(1500.0).save(userRepository);

        List<Long> allIds = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            allIds.add(TestCommunityPuzzleBuilder.builder(user)
                    .withRating(i % 2 == 0 ? 1500.0 : 1650.0)
                    .save(communityPuzzleRepository)
                    .getId());
        }

        // When: walk the cursor three at a time
        List<Long> paged = new ArrayList<>();
        Long cursor = null;
        for (int page = 0; page < 5; page++) {
            List<Long> chunk = recommendIds(user, 99, cursor, 3);
            if (chunk.isEmpty()) break;
            paged.addAll(chunk);
            cursor = chunk.get(chunk.size() - 1);
        }

        // Then: every puzzle exactly once, in the same order a single page would give
        assertThat(paged)
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrderElementsOf(allIds)
                .isEqualTo(recommendIds(user, 99, null, 7));
    }

    @Test
    void searchCommunityPuzzles_WhenShuffleSeedExceedsIntRange_ThenStillOrdersDeterministically() {
        // Given: clients reach for Date.now(), which overflows an int
        UserEntity user = TestUserEntityBuilder.builder().withMmr(1500.0).save(userRepository);
        for (int i = 0; i < 5; i++) {
            TestCommunityPuzzleBuilder.builder(user).withRating(1500.0).save(communityPuzzleRepository);
        }
        long epochMillisSeed = 1_760_000_000_000L;

        // When / Then
        assertThat(recommendIds(user, epochMillisSeed, null, 5))
                .hasSize(5)
                .isEqualTo(recommendIds(user, epochMillisSeed, null, 5));
    }

    private List<Long> recommendIds(UserEntity user, long seed, Long cursorId, int size) {
        return communityPuzzleRepository.searchCommunityPuzzles(
                        new GetCommunityPuzzleRequest(
                                cursorId, size, "RECOMMEND", seed,
                                null, null, null, null, null, null),
                        user,
                        seed
                ).stream()
                .map(CommunityPuzzle::getId)
                .toList();
    }
}
