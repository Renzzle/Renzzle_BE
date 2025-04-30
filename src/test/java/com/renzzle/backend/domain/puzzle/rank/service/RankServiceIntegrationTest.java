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
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.util.ELOUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
@Transactional
public class RankServiceIntegrationTest {

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

    @Value("${rank.session.ttl}")
    private long sessionTtl;

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
        // Given - 사용자 및 Redis Key 설정
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

        // 세션 상태 확인
        RankSessionData session = redisTemplate.opsForValue().get(redisKey);
        assertThat(session).isNotNull();
        assertThat(session.isStarted()).isTrue();

        // endRankGame
        RankEndResponse endResponse = rankService.endRankGame(testUser);

        assertThat(endResponse.rating()).isGreaterThanOrEqualTo(0.0);

        // 세션 제거 확인
        assertThat(redisTemplate.opsForValue().get(redisKey)).isNull();
    }

    @Test
    void rank_WhenCorrectFlow_ThenReturnRating() throws InterruptedException {

        RankStartResponse startResponse = rankService.startRankGame(testUser);
        UserEntity beforeUser = userRepository.findById(testUser.getId()).orElseThrow();
        RankSessionData sessionAfterStart = redisTemplate.opsForValue().get(redisKey);
        assertNotNull(sessionAfterStart, "start 후 세션이 Redis에 존재해야 함");

        assertEquals(startResponse.boardStatus(), sessionAfterStart.getBoardState());

        double ratingAfterStart = beforeUser.getRating();
        double mmrAfterStart = beforeUser.getMmr();
        assertTrue(ratingAfterStart < 1500, "레이팅 감산 확인");
        assertTrue(mmrAfterStart < 1500, "MMR 감산 확인");

        Thread.sleep(1000);

        // result API 호출 - 문제가 정답이라고 가정
        RankResultRequest resultRequest = new RankResultRequest(true);
        RankResultResponse resultResponse = rankService.resultRankGame(beforeUser, resultRequest);

        RankSessionData sessionAfterResult = redisTemplate.opsForValue().get(redisKey);
        assertNotNull(sessionAfterResult, "result 호출 후에도 세션이 Redis에 존재해야 함");
        assertNotEquals(sessionAfterResult.getBoardState(),
                sessionAfterStart.getBoardState(),
                "보드 상태가 동일; 예상과 달리 갱신되지 않았을 수 있음 → " +
                        "start=" + sessionAfterStart.getBoardState() + ", result=" + sessionAfterResult.getBoardState()
        );

        userRepository.flush();
        em.flush();
        em.clear();

        // end API 호출
        RankEndResponse endResponse = rankService.endRankGame(testUser);
        assertEquals(testUser.getRating(), endResponse.rating(), 0.01);

        RankSessionData sessionAfterEnd = redisTemplate.opsForValue().get(redisKey);
        assertNull(sessionAfterEnd, "end 호출 후 Redis에서 세션이 사라져야 함");
    }

    @Test
    void getNextPuzzle_WhenCalled_ThenReturnsNonDuplicateCorrectPuzzle() {

        em.flush();
        em.clear();

        double targetWinProb = 0.7;

        NextPuzzleResult firstResult = rankService.getNextPuzzle(testUser.getMmr(), targetWinProb, testUser);
        LatestRankPuzzle firstPuzzle = firstResult.latestPuzzle();

        // 저장 → 중복 방지를 위해
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

        double newMmr = testUser.getMmr() + ELOUtil.calculateMMRIncrease(testUser.getMmr(), firstResult.rating());
        testUser.updateMmrTo(newMmr);
        userRepository.save(testUser);

        em.flush();
        em.clear();

        NextPuzzleResult secondResult = rankService.getNextPuzzle(testUser.getMmr(), targetWinProb - 0.05, testUser);
        LatestRankPuzzle secondPuzzle = secondResult.latestPuzzle();

        assertNotEquals(firstPuzzle.getBoardStatus(), secondPuzzle.getBoardStatus(), "같은 문제 다시 출제되면 안 됨");

        double diff = Math.abs(secondResult.rating() - ELOUtil.getProblemRatingForTargetWinProbability(testUser.getMmr(), targetWinProb - 0.05));
        assertTrue(diff <= 200, "두 번째 문제의 레이팅은 기대값 근처여야 함");
    }
}
