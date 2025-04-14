package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.puzzle.rank.api.request.RankResultRequest;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankEndResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankResultResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankStartResponse;
import com.renzzle.backend.domain.puzzle.rank.domain.LatestRankPuzzle;
import com.renzzle.backend.domain.puzzle.rank.domain.RankSessionData;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.puzzle.rank.util.PuzzleSeeder;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.util.ELOUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.renzzle.backend.global.common.constant.TimeConstant.CONST_FUTURE_INSTANT;
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
    @Autowired private PuzzleSeeder puzzleSeeder;
    @Autowired private Clock clock;

    @PersistenceContext
    private EntityManager em;

    private UserEntity testUser;
    private String redisKey;

    @Value("${rank.session.ttl}")
    private long sessionTtl;

    @BeforeEach
    void setup() {
//        testUser = createTestUser(1500, 1500);
        testUser = userRepository.save(TestUserFactory.createTestUser("tester", 1500));
        userRepository.flush();
        em.flush();
        em.clear();
        redisKey = String.valueOf(testUser.getId());

        puzzleSeeder.seedPuzzle(1, "a1a2", "a3",3,  1400, "BLACK");
        puzzleSeeder.seedPuzzle(2, "b1b2", "b3", 3, 1450, "WHITE");
        puzzleSeeder.seedPuzzle(3, "c1c2", "c3", 4, 1500, "BLACK");
        puzzleSeeder.seedPuzzle(4, "d1d2", "d3", 5, 1550, "WHITE");
        puzzleSeeder.seedPuzzle(5, "e1e2", "e3", 6, 1600, "BLACK");
        puzzleSeeder.seedPuzzle(6, "a1a2a3", "a13",3,  1353, "BLACK");

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
    void getNextPuzzle_ShouldReturnCorrectPuzzleAndAvoidDuplicates() {

        em.flush();
        em.clear();

        double targetWinProb = 0.7;
        TrainingPuzzle firstPuzzle = rankService.getNextPuzzle(testUser.getMmr(), targetWinProb, testUser);

        assertEquals(1353, firstPuzzle.getRating(), "첫 번째 문제는 1353이어야 함");

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

        double newMmr = testUser.getMmr() + ELOUtil.calculateMMRIncrease(testUser.getMmr(), firstPuzzle.getRating());
        testUser.updateMmrTo(newMmr);
        userRepository.save(testUser);

        em.flush();
        em.clear();

        TrainingPuzzle secondPuzzle = rankService.getNextPuzzle(testUser.getMmr(), targetWinProb - 0.05, testUser);

        assertNotEquals(firstPuzzle.getId(), secondPuzzle.getId(), "같은 문제 다시 출제되면 안 됨");
        assertEquals(1400, secondPuzzle.getRating(), "두 번째 문제는 1400이어야 함");
    }

}
