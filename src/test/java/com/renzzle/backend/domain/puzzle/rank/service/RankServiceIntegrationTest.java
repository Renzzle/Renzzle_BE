package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.rank.api.request.RankResultRequest;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankEndResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankResultResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankStartResponse;
import com.renzzle.backend.domain.puzzle.rank.dao.LatestRankPuzzleRepository;
import com.renzzle.backend.domain.puzzle.rank.domain.LatestRankPuzzle;
import com.renzzle.backend.domain.puzzle.rank.domain.RankSessionData;
import com.renzzle.backend.domain.puzzle.rank.service.dto.NextPuzzleResult;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.puzzle.rank.util.CommunityPuzzleSeeder;
import com.renzzle.backend.domain.puzzle.rank.util.TrainingPuzzleSeeder;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.domain.puzzle.shared.util.ELOUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.UUID;

import static com.renzzle.backend.global.common.constant.ItemPrice.RANK_REWARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
class RankServiceIntegrationTest {

    @Autowired private RankService rankService;
    @Autowired private UserRepository userRepository;
    @Autowired private LatestRankPuzzleRepository latestRankPuzzleRepository;
    @Autowired private RedisTemplate<String, RankSessionData> redisTemplate;
    @Autowired private TrainingPuzzleSeeder trainingPuzzleSeeder;
    @Autowired private CommunityPuzzleSeeder communityPuzzleSeeder;
    @Autowired private TrainingPuzzleRepository trainingPuzzleRepository;
    @Autowired private CommunityPuzzleRepository communityPuzzleRepository;
    @Autowired private Clock clock;
    @Autowired private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager em;

    private UserEntity testUser;
    private String redisKey;

    // These tests commit to a shared container, so clear puzzle tables before and after each one
    private void clearPuzzleData() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            em.createNativeQuery("DELETE FROM latest_rank_puzzle").executeUpdate();
            em.createNativeQuery("DELETE FROM user_community_puzzle").executeUpdate();
            em.createNativeQuery("DELETE FROM community_puzzle").executeUpdate();
            em.createNativeQuery("DELETE FROM training_puzzle").executeUpdate();
            em.createNativeQuery("DELETE FROM pack").executeUpdate();
        });
    }

    @AfterEach
    void tearDown() {
        clearPuzzleData();
        if (testUser != null && testUser.getId() != null) {
            new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                    em.createNativeQuery("DELETE FROM user WHERE id = :id")
                            .setParameter("id", testUser.getId())
                            .executeUpdate());
        }
        redisTemplate.delete(redisKey);
    }

    @BeforeEach
    void setup() {
        clearPuzzleData();

        testUser = userRepository.save(TestUserFactory.createTestUser("tester-" + UUID.randomUUID().toString().substring(0, 8), 1500));
        redisKey = String.valueOf(testUser.getId());
        redisTemplate.delete(redisKey);

        trainingPuzzleSeeder.seedPuzzle(1, "a1a2", "a3", 3, 1400, "BLACK");
        trainingPuzzleSeeder.seedPuzzle(2, "b1b2", "b3", 3, 1450, "WHITE");

        communityPuzzleSeeder.seedPuzzle("c1c2", "c3", 4, 1500, "BLACK", testUser);
        communityPuzzleSeeder.seedPuzzle("d1d2", "d3", 5, 1550, "WHITE", testUser);
        communityPuzzleSeeder.seedPuzzle("e1e2", "e3", 6, 1600, "BLACK", testUser);
        communityPuzzleSeeder.seedPuzzle("a1a2a3", "a13", 3, 1353, "BLACK", testUser);
    }

    @Test
    void rankingFlow_WhenTrainingAndCommunityPuzzlesGiven_ThenCompleteSuccessfully() {
        // startRankGame
        RankStartResponse startResponse = rankService.startRankGame(testUser);

        assertThat(startResponse.boardStatus()).isNotBlank();
        assertThat(startResponse.winColor()).isNotBlank();

        // resultRankGame
        RankResultRequest resultRequest = new RankResultRequest(true, null);
        RankResultResponse resultResponse = rankService.resultRankGame(testUser, resultRequest);

        assertThat(resultResponse.boardStatus()).isNotBlank();
        assertThat(resultResponse.winColor()).isNotBlank();

        // Check session state
        RankSessionData session = redisTemplate.opsForValue().get(redisKey);
        assertThat(session).isNotNull();
        assertThat(session.isStarted()).isTrue();

        // endRankGame
        RankEndResponse endResponse = rankService.endRankGame(testUser);

        assertThat(endResponse.rating()).isGreaterThanOrEqualTo(0.0);

        // Check session removal
        assertThat(redisTemplate.opsForValue().get(redisKey)).isNull();
    }

    @Test
    void rank_WhenCorrectFlow_ThenReturnRating() {

        RankStartResponse startResponse = rankService.startRankGame(testUser);
        UserEntity afterStart = userRepository.findById(testUser.getId()).orElseThrow();
        RankSessionData sessionAfterStart = redisTemplate.opsForValue().get(redisKey);
        assertNotNull(sessionAfterStart, "session must exist in Redis after start");

        assertEquals(startResponse.boardStatus(), sessionAfterStart.getBoardState());

        assertTrue(afterStart.getRating() < 1500, "rating must be deducted");
        assertTrue(afterStart.getMmr() < 1500, "mmr must be deducted");

        // Call result API - assume the problem is answered correctly
        RankResultRequest resultRequest = new RankResultRequest(true, null);
        rankService.resultRankGame(testUser, resultRequest);

        RankSessionData sessionAfterResult = redisTemplate.opsForValue().get(redisKey);
        assertNotNull(sessionAfterResult, "session must still exist in Redis after result");
        assertNotEquals(sessionAfterResult.getBoardState(),
                sessionAfterStart.getBoardState(),
                "board state unchanged; it may not have been refreshed as expected -> " +
                        "start=" + sessionAfterStart.getBoardState() + ", result=" + sessionAfterResult.getBoardState()
        );

        // Call end API
        RankEndResponse endResponse = rankService.endRankGame(testUser);
        double storedRating = userRepository.findById(testUser.getId()).orElseThrow().getRating();
        assertEquals(storedRating, endResponse.rating(), 0.01);

        RankSessionData sessionAfterEnd = redisTemplate.opsForValue().get(redisKey);
        assertNull(sessionAfterEnd, "session must be gone from Redis after end");
    }

    @Test
    void resultRankGame_WhenSessionIsLost_ThenRatingStillRevertsFromTheAssignmentRow() {
        rankService.startRankGame(testUser);
        double ratingAfterStart = userRepository.findById(testUser.getId()).orElseThrow().getRating();
        assertTrue(ratingAfterStart < 1500, "the assignment must pre-deduct the loss");

        // Lose the session like a Redis restart would, keeping only the liveness marker
        LatestRankPuzzle assigned = latestRankPuzzleRepository.findTopByUserOrderByIdDesc(testUser).orElseThrow();
        redisTemplate.delete(redisKey);

        RankSessionData revived = new RankSessionData();
        revived.setUserId(testUser.getId());
        revived.setStarted(true);
        redisTemplate.opsForValue().set(redisKey, revived, 60, java.util.concurrent.TimeUnit.SECONDS);

        rankService.resultRankGame(testUser, new RankResultRequest(true, null));

        double expectedAfterSolve = assigned.getRatingBeforePenalty()
                + ELOUtils.calculateRatingIncrease(assigned.getRatingBeforePenalty(), assigned.getPuzzleRating());
        double actual = userRepository.findById(testUser.getId()).orElseThrow().getRating();

        // The next puzzle's pre-deduction is applied on top, so compare against the solve alone
        LatestRankPuzzle nextAssigned = latestRankPuzzleRepository.findTopByUserOrderByIdDesc(testUser).orElseThrow();
        assertEquals(expectedAfterSolve, nextAssigned.getRatingBeforePenalty(), 0.01);
        assertEquals(expectedAfterSolve
                        + ELOUtils.calculateRatingDecrease(expectedAfterSolve, nextAssigned.getPuzzleRating()),
                actual, 0.01);
    }

    @Test
    void rankFlow_WhenPuzzleAnswered_ThenOnlyTheAnsweredPuzzleRatingMoves() {
        // The second puzzle is left unanswered when the game ends
        rankService.startRankGame(testUser);
        LatestRankPuzzle answered = latestRankPuzzleRepository.findTopByUserOrderByIdDesc(testUser).orElseThrow();

        rankService.resultRankGame(testUser, new RankResultRequest(false, null));
        LatestRankPuzzle unanswered = latestRankPuzzleRepository.findTopByUserOrderByIdDesc(testUser).orElseThrow();
        double unansweredBefore = currentRating(unanswered);

        rankService.endRankGame(testUser);

        double expected = answered.getPuzzleRating() + ELOUtils.calculatePuzzleRatingChange(
                answered.getPuzzleType(), answered.getMmrBeforePenalty(), answered.getPuzzleRating(), 0, false);
        assertEquals(expected, currentRating(answered), 0.0001);
        assertEquals(1, currentAttempts(answered));

        assertEquals(unansweredBefore, currentRating(unanswered), 0.0001);
        assertEquals(0, currentAttempts(unanswered));
    }

    private double currentRating(LatestRankPuzzle assignment) {
        return switch (assignment.getPuzzleType()) {
            case TRAINING -> trainingPuzzle(assignment).getRating();
            case COMMUNITY -> communityPuzzle(assignment).getRating();
        };
    }

    private int currentAttempts(LatestRankPuzzle assignment) {
        return switch (assignment.getPuzzleType()) {
            case TRAINING -> trainingPuzzle(assignment).getRankAttemptCount();
            case COMMUNITY -> communityPuzzle(assignment).getRankAttemptCount();
        };
    }

    private TrainingPuzzle trainingPuzzle(LatestRankPuzzle assignment) {
        return trainingPuzzleRepository.findById(assignment.getPuzzleId()).orElseThrow();
    }

    private CommunityPuzzle communityPuzzle(LatestRankPuzzle assignment) {
        return communityPuzzleRepository.findById(assignment.getPuzzleId()).orElseThrow();
    }

    @Test
    void endRankGame_WhenSolvedPuzzlesExist_ThenReturnCorrectReward() {
        // Given
        latestRankPuzzleRepository.save(LatestRankPuzzle.builder()
                .user(testUser)
                .boardStatus("p1")
                .answer("a1")
                .winColor(WinColor.getWinColor("BLACK"))
                .isSolved(true)
                .assignedAt(clock.instant())
                .build());

        latestRankPuzzleRepository.save(LatestRankPuzzle.builder()
                .user(testUser)
                .boardStatus("p2")
                .answer("a2")
                .winColor(WinColor.getWinColor("WHITE"))
                .isSolved(true)
                .assignedAt(clock.instant())
                .build());

        RankSessionData session = new RankSessionData();
        session.setStarted(true);
        redisTemplate.opsForValue().set(redisKey, session);

        // When
        RankEndResponse response = rankService.endRankGame(testUser);

        // Then
        assertThat(response.rating()).isEqualTo(testUser.getRating());
        assertThat(response.reward()).isEqualTo(2 * RANK_REWARD.getDefaultPrice()); // 2 correct answers
    }

    @Test
    void resultRankGame_WhenResentAfterALostResponse_ThenCountsTheAnswerOnce() {
        // Given: the first result went through, but the app never saw its response
        RankStartResponse start = rankService.startRankGame(testUser);
        RankResultResponse next = rankService.resultRankGame(testUser, new RankResultRequest(true, start.boardStatus()));
        double ratingAfterAnswer = userRepository.findById(testUser.getId()).orElseThrow().getRating();

        // When: the app, still on the first board, sends a result again
        RankResultResponse replayed = rankService.resultRankGame(testUser, new RankResultRequest(false, start.boardStatus()));

        // Then: it gets the puzzle it missed, and nothing else moves
        assertThat(replayed.boardStatus()).isEqualTo(next.boardStatus());
        assertThat(userRepository.findById(testUser.getId()).orElseThrow().getRating()).isEqualTo(ratingAfterAnswer);
        assertThat(latestRankPuzzleRepository.findAllByUser(testUser)).hasSize(2);
    }

    @Test
    void getRankArchive_WhenGameInProgress_ThenHidesOnlyThePuzzleBeingSolved() {
        // The puzzle on the board stays out of the archive until the game ends
        rankService.startRankGame(testUser);
        assertThat(rankService.getRankArchive(testUser)).isEmpty();

        rankService.resultRankGame(testUser, new RankResultRequest(true, null));
        assertThat(rankService.getRankArchive(testUser)).hasSize(1);

        rankService.endRankGame(testUser);
        assertThat(rankService.getRankArchive(testUser)).hasSize(2);
    }

    @Test
    void getNextPuzzle_WhenCalled_ThenReturnsNonDuplicateCorrectPuzzle() {
        double targetWinProb = 0.7;

        NextPuzzleResult firstResult = rankService.getNextPuzzle(testUser.getMmr(), targetWinProb, testUser);

        // Save -> to prevent duplicates
        latestRankPuzzleRepository.save(LatestRankPuzzle.builder()
                .user(testUser)
                .boardStatus(firstResult.boardStatus())
                .answer(firstResult.answer())
                .isSolved(true)
                .assignedAt(clock.instant())
                .winColor(firstResult.winColor())
                .build());

        double newMmr = testUser.getMmr() + ELOUtils.calculateMMRIncrease(testUser.getMmr(), firstResult.rating());
        testUser.updateMmrTo(newMmr);
        userRepository.save(testUser);

        NextPuzzleResult secondResult = rankService.getNextPuzzle(testUser.getMmr(), targetWinProb - 0.05, testUser);

        assertNotEquals(firstResult.boardStatus(), secondResult.boardStatus(), "the same puzzle must not be served twice");
    }
}
