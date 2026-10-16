package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.rank.dao.LatestRankPuzzleRepository;
import com.renzzle.backend.domain.puzzle.rank.domain.LatestRankPuzzle;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.puzzle.rank.util.CommunityPuzzleSeeder;
import com.renzzle.backend.domain.puzzle.rank.util.PackSeeder;
import com.renzzle.backend.domain.puzzle.rank.util.TrainingPuzzleSeeder;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.shared.dto.RankPuzzleCandidate;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.support.DataJpaTestWithInitContainers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Limit;

import java.util.List;
import java.util.Optional;

import static com.renzzle.backend.domain.puzzle.shared.util.RatingUtil.MAX_RATING;
import static com.renzzle.backend.domain.puzzle.shared.util.RatingUtil.MIN_RATING;
import static com.renzzle.backend.support.TestTime.FIXED_INSTANT;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithInitContainers
@Import({TrainingPuzzleSeeder.class, CommunityPuzzleSeeder.class, PackSeeder.class})
class RankRepositoryTest {

    @Autowired
    private TrainingPuzzleSeeder trainingPuzzleSeeder;

    @Autowired
    private CommunityPuzzleSeeder communityPuzzleSeeder;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrainingPuzzleRepository trainingPuzzleRepository;

    @Autowired
    private CommunityPuzzleRepository communityPuzzleRepository;

    @Autowired
    private LatestRankPuzzleRepository latestRankPuzzleRepository;

    @Autowired
    private EntityManager em;

    @Test
    void saveLatestRankPuzzle_WhenTrainingPuzzleGiven_ThenSaveSuccessfully() {
        // Create user
        UserEntity user = userRepository.save(TestUserFactory.createTestUser("seeder-user", 1500));

        // Create puzzles via seeder
        trainingPuzzleSeeder.seedPuzzle(1, "a1a2", "a3", 3, 1400, "BLACK");
        communityPuzzleSeeder.seedPuzzle("b1b2", "b3", 4, 1450, "WHITE", user);

        // Look up the puzzle directly and create a ranking record
        TrainingPuzzle training = trainingPuzzleRepository.findAll().get(0);
        LatestRankPuzzle latest = LatestRankPuzzle.builder()
                .user(user)
                .boardStatus(training.getBoardStatus())
                .answer(training.getAnswer())
                .winColor(training.getWinColor())
                .assignedAt(FIXED_INSTANT)
                .isSolved(false)
                .build();
        latestRankPuzzleRepository.save(latest);

        // Verify
        List<LatestRankPuzzle> all = latestRankPuzzleRepository.findAllByUser(user);
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getBoardStatus()).isEqualTo("a1a2");
    }

    @Test
    void saveLatestRankPuzzle_WhenCommunityPuzzleIsGiven_ThenSaveSuccessfully() {
        // Given
        UserEntity user = userRepository.save(TestUserFactory.createTestUser("author", 1500));

        // Create community puzzle via seeder
        communityPuzzleSeeder.seedPuzzle(
                "b1b2", "b3", 4, 1450.0, "WHITE", user
        );
        CommunityPuzzle community = communityPuzzleRepository.findAll().get(0);

        // Save ranking puzzle
        LatestRankPuzzle latest = latestRankPuzzleRepository.save(
                LatestRankPuzzle.builder()
                        .user(user)
                        .boardStatus(community.getBoardStatus())
                        .answer(community.getAnswer())
                        .winColor(community.getWinColor())
                        .assignedAt(FIXED_INSTANT)
                        .isSolved(false)
                        .build()
        );

        // When
        Optional<LatestRankPuzzle> saved = latestRankPuzzleRepository.findTopByUserOrderByIdDesc(user);

        // Then
        assertThat(saved).isPresent();
        assertThat(saved.get().getBoardStatus()).isEqualTo("b1b2");
        assertThat(saved.get().getIsSolved()).isFalse();
    }

    @Test
    void applyRankResult_WhenTrainingPuzzleAnswered_ThenMovesRatingAndCountsTheAttempt() {
        // Given
        trainingPuzzleSeeder.seedPuzzle(1, "a1a2", "a3", 3, 1400, "BLACK");
        Long id = trainingPuzzleId("a1a2");

        // When
        trainingPuzzleRepository.applyRankResult(id, 25.5, MIN_RATING, MAX_RATING);
        trainingPuzzleRepository.applyRankResult(id, -10.0, MIN_RATING, MAX_RATING);
        em.clear();

        // Then
        TrainingPuzzle updated = trainingPuzzleRepository.findById(id).orElseThrow();
        assertThat(updated.getRating()).isEqualTo(1415.5);
        assertThat(updated.getRankAttemptCount()).isEqualTo(2);
        assertThat(trainingPuzzleRepository.findRankAttemptCountById(id)).contains(2);
    }

    @Test
    void applyRankResult_WhenDeltaCrossesABound_ThenStopsAtThatBound() {
        // Given
        UserEntity author = userRepository.save(TestUserFactory.createTestUser("author", 1500));
        trainingPuzzleSeeder.seedPuzzle(1, "a1a2", "a3", 3, MAX_RATING - 10, "BLACK");
        communityPuzzleSeeder.seedPuzzle("b1b2", "b3", 4, MIN_RATING + 10, "WHITE", author);
        Long trainingId = trainingPuzzleId("a1a2");
        Long communityId = communityPuzzleId("b1b2");

        // When
        trainingPuzzleRepository.applyRankResult(trainingId, 40.0, MIN_RATING, MAX_RATING);
        communityPuzzleRepository.applyRankResult(communityId, -40.0, MIN_RATING, MAX_RATING);
        em.clear();

        // Then
        assertThat(trainingPuzzleRepository.findById(trainingId).orElseThrow().getRating()).isEqualTo(MAX_RATING);
        CommunityPuzzle community = communityPuzzleRepository.findById(communityId).orElseThrow();
        assertThat(community.getRating()).isEqualTo(MIN_RATING);
        assertThat(community.getRankAttemptCount()).isEqualTo(1);
    }

    @Test
    void findRankAttemptCountById_WhenCommunityPuzzleDeleted_ThenReturnsEmpty() {
        // Given
        UserEntity author = userRepository.save(TestUserFactory.createTestUser("author", 1500));
        communityPuzzleSeeder.seedPuzzle("b1b2", "b3", 4, 1450, "WHITE", author);
        Long id = communityPuzzleId("b1b2");
        assertThat(communityPuzzleRepository.findRankAttemptCountById(id)).contains(0);

        // When
        communityPuzzleRepository.softDelete(id, FIXED_INSTANT);
        em.clear();

        // Then
        assertThat(communityPuzzleRepository.findRankAttemptCountById(id)).isEmpty();
    }

    // Ratings far from every other test's puzzles, so only the ones seeded here are within reach
    @Test
    void findRankCandidates_WhenTargetGiven_ThenReturnsTheNearestOnEachSideInOrder() {
        UserEntity user = userRepository.save(TestUserFactory.createTestUser("nearest", 1500));
        trainingPuzzleSeeder.seedPuzzle(1, "a1a2", "a3", 3, 9010, "BLACK");
        trainingPuzzleSeeder.seedPuzzle(1, "b1b2", "b3", 3, 9030, "BLACK");
        trainingPuzzleSeeder.seedPuzzle(1, "c1c2", "c3", 3, 9000, "WHITE");
        trainingPuzzleSeeder.seedPuzzle(1, "d1d2", "d3", 3, 8990, "BLACK");
        trainingPuzzleSeeder.seedPuzzle(1, "e1e2", "e3", 3, 8950, "BLACK");

        List<RankPuzzleCandidate> from = trainingPuzzleRepository.findRankCandidatesFrom(user, 9000, Limit.of(2));
        List<RankPuzzleCandidate> below = trainingPuzzleRepository.findRankCandidatesBelow(user, 9000, Limit.of(2));

        assertThat(from).extracting(RankPuzzleCandidate::boardStatus).containsExactly("c1c2", "a1a2");
        assertThat(from.get(0).winColor()).isEqualTo("WHITE");
        assertThat(below).extracting(RankPuzzleCandidate::boardStatus).containsExactly("d1d2", "e1e2");
    }

    @Test
    void findRankCandidates_WhenNearlyEveryPuzzleWasHandedOut_ThenStillReachesTheFarOneLeft() {
        UserEntity user = userRepository.save(TestUserFactory.createTestUser("nearly-done", 1500));
        trainingPuzzleSeeder.seedPuzzle(1, "a1a2", "a3", 3, 9000, "BLACK");
        trainingPuzzleSeeder.seedPuzzle(1, "b1b2", "b3", 3, 9010, "BLACK");
        trainingPuzzleSeeder.seedPuzzle(1, "c1c2", "c3", 3, 8600, "BLACK");
        handOut(user, "a1a2");
        handOut(user, "b1b2");

        assertThat(trainingPuzzleRepository.findRankCandidatesFrom(user, 9000, Limit.of(5))).isEmpty();
        assertThat(trainingPuzzleRepository.findRankCandidatesBelow(user, 9000, Limit.of(1)))
                .extracting(RankPuzzleCandidate::boardStatus)
                .containsExactly("c1c2");
    }

    @Test
    void findRankCandidates_WhenCommunityPuzzleUnverified_ThenLeavesItOut() {
        UserEntity author = userRepository.save(TestUserFactory.createTestUser("verified-only", 1500));
        communityPuzzleSeeder.seedPuzzle("a1a2", "a3", 3, 9000, "BLACK", author);
        communityPuzzleSeeder.seedPuzzle("b1b2", "b3", 3, 9001, "WHITE", author);
        communityPuzzleRepository.findById(communityPuzzleId("b1b2")).orElseThrow().updateVerification(false);
        em.flush();

        assertThat(communityPuzzleRepository.findRankCandidatesFrom(author, 9000, Limit.of(5)))
                .extracting(RankPuzzleCandidate::boardStatus)
                .containsExactly("a1a2");
        assertThat(communityPuzzleRepository.findAllRankCandidates())
                .extracting(RankPuzzleCandidate::boardStatus)
                .contains("a1a2")
                .doesNotContain("b1b2");
    }

    @Test
    void findBoardStatusesByUser_WhenPuzzlesHandedOut_ThenListsThemInHandOutOrder() {
        UserEntity user = userRepository.save(TestUserFactory.createTestUser("hand-out-order", 1500));
        handOut(user, "b1b2");
        handOut(user, "a1a2");
        handOut(user, "b1b2");

        assertThat(latestRankPuzzleRepository.findBoardStatusesByUser(user)).containsExactly("b1b2", "a1a2", "b1b2");
    }

    private void handOut(UserEntity user, String boardStatus) {
        latestRankPuzzleRepository.save(LatestRankPuzzle.builder()
                .user(user)
                .boardStatus(boardStatus)
                .answer("x")
                .winColor(WinColor.getWinColor("BLACK"))
                .assignedAt(FIXED_INSTANT)
                .isSolved(false)
                .build());
    }

    private Long trainingPuzzleId(String boardStatus) {
        return trainingPuzzleRepository.findAll().stream()
                .filter(p -> p.getBoardStatus().equals(boardStatus))
                .findFirst().orElseThrow().getId();
    }

    private Long communityPuzzleId(String boardStatus) {
        return communityPuzzleRepository.findAll().stream()
                .filter(p -> p.getBoardStatus().equals(boardStatus))
                .findFirst().orElseThrow().getId();
    }
}
