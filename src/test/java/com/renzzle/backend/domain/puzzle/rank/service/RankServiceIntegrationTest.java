package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.config.TestContainersConfig;
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

/*
    Deliberately not @Transactional: the service defers its Redis writes to afterCommit, so a
    test-managed transaction that always rolls back would hide every session write. Each test
    commits for real and cleans up beforehand instead.
*/
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
    @Autowired private Clock clock;
    @Autowired private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager em;

    private UserEntity testUser;
    private String redisKey;

    /*
        These tests commit, and the MySQL container is shared with the rollback-based repository
        tests, so anything left behind would leak into them. Clear the puzzle tables on both sides
        of every test. Reference data seeded by DataInitializer is untouched.
    */
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
        RankResultRequest resultRequest = new RankResultRequest(true);
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
        RankResultRequest resultRequest = new RankResultRequest(true);
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

        // Wipe the session the way a Redis restart or an eviction would, then restore only the
        // liveness marker. The pre-deduction values live on the assignment row, so a solve must
        // still revert the deduction rather than compound it.
        LatestRankPuzzle assigned = latestRankPuzzleRepository.findTopByUserOrderByIdDesc(testUser).orElseThrow();
        redisTemplate.delete(redisKey);

        RankSessionData revived = new RankSessionData();
        revived.setUserId(testUser.getId());
        revived.setStarted(true);
        redisTemplate.opsForValue().set(redisKey, revived, 60, java.util.concurrent.TimeUnit.SECONDS);

        rankService.resultRankGame(testUser, new RankResultRequest(true));

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
        assertThat(response.reward()).isEqualTo(2 * RANK_REWARD.getPrice()); // 2 correct answers
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
