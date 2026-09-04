package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.puzzle.rank.api.request.RankResultRequest;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankEndResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankResultResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankStartResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

import static com.renzzle.backend.global.common.constant.ItemPrice.RANK_REWARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
@Transactional
class RankServiceIntegrationTest {

    @Autowired private RankService rankService;
    @Autowired private UserRepository userRepository;
    @Autowired private RedisTemplate<String, RankSessionData> redisTemplate;
    @Autowired private TrainingPuzzleSeeder trainingPuzzleSeeder;
    @Autowired private CommunityPuzzleSeeder communityPuzzleSeeder;
    @Autowired private Clock clock;

    @PersistenceContext
    private EntityManager em;

    private UserEntity testUser;
    private String redisKey;

    @BeforeEach
    void setup() {
        testUser = userRepository.save(TestUserFactory.createTestUser("tester", 1500));
        userRepository.flush();
        em.flush();
        em.clear();
        redisKey = String.valueOf(testUser.getId());

        trainingPuzzleSeeder.seedPuzzle(1, "a1a2", "a3", 3, 1400, "BLACK");
        trainingPuzzleSeeder.seedPuzzle(2, "b1b2", "b3", 3, 1450, "WHITE");

        communityPuzzleSeeder.seedPuzzle("c1c2", "c3", 4, 1500, "BLACK", testUser);
        communityPuzzleSeeder.seedPuzzle("d1d2", "d3", 5, 1550, "WHITE", testUser);
        communityPuzzleSeeder.seedPuzzle("e1e2", "e3", 6, 1600, "BLACK", testUser);
        communityPuzzleSeeder.seedPuzzle("a1a2a3", "a13", 3, 1353, "BLACK", testUser);

    }

    @Test
    void rankingFlow_WhenTrainingAndCommunityPuzzlesGiven_ThenCompleteSuccessfully() {
        // Given - set up user and Redis Key
        Long userId = testUser.getId();
        String redisKey = String.valueOf(userId);

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
    void rank_WhenCorrectFlow_ThenReturnRating() throws InterruptedException {

        RankStartResponse startResponse = rankService.startRankGame(testUser);
        UserEntity beforeUser = userRepository.findById(testUser.getId()).orElseThrow();
        RankSessionData sessionAfterStart = redisTemplate.opsForValue().get(redisKey);
        assertNotNull(sessionAfterStart, "session must exist in Redis after start");

        assertEquals(startResponse.boardStatus(), sessionAfterStart.getBoardState());

        double ratingAfterStart = beforeUser.getRating();
        double mmrAfterStart = beforeUser.getMmr();
        assertTrue(ratingAfterStart < 1500, "rating must be deducted");
        assertTrue(mmrAfterStart < 1500, "mmr must be deducted");

        Thread.sleep(1000);

        // Call result API - assume the problem is answered correctly
        RankResultRequest resultRequest = new RankResultRequest(true);
        rankService.resultRankGame(beforeUser, resultRequest);

        RankSessionData sessionAfterResult = redisTemplate.opsForValue().get(redisKey);
        assertNotNull(sessionAfterResult, "session must still exist in Redis after result");
        assertNotEquals(sessionAfterResult.getBoardState(),
                sessionAfterStart.getBoardState(),
                "board state unchanged; it may not have been refreshed as expected -> " +
                        "start=" + sessionAfterStart.getBoardState() + ", result=" + sessionAfterResult.getBoardState()
        );

        userRepository.flush();
        em.flush();
        em.clear();

        // Call end API
        RankEndResponse endResponse = rankService.endRankGame(testUser);
        assertEquals(testUser.getRating(), endResponse.rating(), 0.01);

        RankSessionData sessionAfterEnd = redisTemplate.opsForValue().get(redisKey);
        assertNull(sessionAfterEnd, "session must be gone from Redis after end");
    }

    @Test
    void endRankGame_WhenSolvedPuzzlesExist_ThenReturnCorrectReward() {
        // Given
        LatestRankPuzzle solved1 = LatestRankPuzzle.builder()
                .user(testUser)
                .boardStatus("p1")
                .answer("a1")
                .winColor(WinColor.getWinColor("BLACK"))
                .isSolved(true)
                .assignedAt(clock.instant())
                .build();

        LatestRankPuzzle solved2 = LatestRankPuzzle.builder()
                .user(testUser)
                .boardStatus("p2")
                .answer("a2")
                .winColor(WinColor.getWinColor("WHITE"))
                .isSolved(true)
                .assignedAt(clock.instant())
                .build();

        em.persist(solved1);
        em.persist(solved2);
        em.flush();
        em.clear();

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

        em.flush();
        em.clear();

        double targetWinProb = 0.7;

        NextPuzzleResult firstResult = rankService.getNextPuzzle(testUser.getMmr(), targetWinProb, testUser);
        LatestRankPuzzle firstPuzzle = firstResult.latestPuzzle();

        // Save -> to prevent duplicates
        LatestRankPuzzle solved = LatestRankPuzzle.builder()
                .user(testUser)
                .boardStatus(firstPuzzle.getBoardStatus())
                .answer(firstPuzzle.getAnswer())
                .isSolved(true)
                .assignedAt(clock.instant())
                .winColor(firstPuzzle.getWinColor())
                .build();

        em.persist(solved);
        em.flush();
        em.clear();

        double newMmr = testUser.getMmr() + ELOUtils.calculateMMRIncrease(testUser.getMmr(), firstResult.rating());
        testUser.updateMmrTo(newMmr);
        userRepository.save(testUser);

        em.flush();
        em.clear();

        NextPuzzleResult secondResult = rankService.getNextPuzzle(testUser.getMmr(), targetWinProb - 0.05, testUser);
        LatestRankPuzzle secondPuzzle = secondResult.latestPuzzle();

        assertNotEquals(firstPuzzle.getBoardStatus(), secondPuzzle.getBoardStatus(), "the same puzzle must not be served twice");
    }
}
