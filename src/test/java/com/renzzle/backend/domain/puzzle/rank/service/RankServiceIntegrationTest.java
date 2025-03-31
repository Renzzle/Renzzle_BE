package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.puzzle.rank.api.request.RankResultRequest;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankEndResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankResultResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankStartResponse;
import com.renzzle.backend.domain.puzzle.rank.domain.RankSessionData;
import com.renzzle.backend.domain.puzzle.rank.util.PuzzleSeeder;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

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


    private UserEntity testUser;
    private String redisKey;

    @BeforeEach
    void setup() {
        testUser = createTestUser(1500, 1500);
        userRepository.flush();
        redisKey = String.valueOf(testUser.getId());

        puzzleSeeder.seedPuzzle(1, "a1a2a3a4", "a4a5", 1, 1400, "BLACK");
        puzzleSeeder.seedPuzzle(2, "b1b2b3b4", "b4b5", 1, 1500, "WHITE");
        puzzleSeeder.seedPuzzle(3, "c1c2c3c4", "c4c5", 1, 1600, "BLACK");
    }

    @Test
    void testFullRankGameFlow_withTTL() throws InterruptedException {

//        long puzzleCount = trainingPuzzleRepository.count();
//        assertEquals(3, puzzleCount, "퍼즐이 정확히 3개 추가되어야 함");

        RankStartResponse startResponse = rankService.startRankGame(testUser);
        RankSessionData sessionAfterStart = redisTemplate.opsForValue().get(redisKey);
        assertNotNull(sessionAfterStart, "start 후 세션이 Redis에 존재해야 함");

        assertEquals(startResponse.boardStatus(), sessionAfterStart.getBoardState());

        double ratingAfterStart = testUser.getRating();
        double mmrAfterStart = testUser.getMmr();
        assertTrue(ratingAfterStart < 1500, "레이팅 감산 확인");
        assertTrue(mmrAfterStart < 1500, "MMR 감산 확인");

        // ⏳ 대기 (TTL 내)
        Thread.sleep(3000);

        // result API 호출 - 정답 가정
        RankResultRequest resultRequest = new RankResultRequest(true);
        RankResultResponse resultResponse = rankService.resultRankGame(testUser, resultRequest);

        RankSessionData sessionAfterResult = redisTemplate.opsForValue().get(redisKey);
        assertNotNull(sessionAfterResult, "result 호출 후에도 세션이 Redis에 존재해야 함");
//        assertNotEquals(sessionAfterResult.getBoardState(), sessionAfterStart.getBoardState());

        double ratingAfterResult = testUser.getRating();
        double mmrAfterResult = testUser.getMmr();
        assertTrue(ratingAfterResult > ratingAfterStart, "정답 시 레이팅 증가");
        assertTrue(mmrAfterResult > mmrAfterStart, "정답 시 MMR 증가");

        // end API 호출
        RankEndResponse endResponse = rankService.endRankGame(testUser);
        assertEquals(testUser.getRating(), endResponse.rating(), 0.01);

        RankSessionData sessionAfterEnd = redisTemplate.opsForValue().get(redisKey);
        assertNull(sessionAfterEnd, "end 호출 후 Redis에서 세션이 사라져야 함");
    }

    @Test
    void testSessionExpiresAfterTTL() throws InterruptedException {
        rankService.startRankGame(testUser);

        // TTL 확인
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        assertNotNull(ttl);
        assertTrue(ttl <= 10, "TTL이 설정되어 있어야 함");

        // 11초 대기
        Thread.sleep(11000);

        RankSessionData expiredSession = redisTemplate.opsForValue().get(redisKey);
        assertNull(expiredSession, "TTL 초과 후 Redis 세션은 null이어야 함");
    }

    // 🔧 테스트 유저 생성 도우미
    private UserEntity createTestUser(double rating, double mmr) {
        return userRepository.save(
                UserEntity.builder()
                        .email("test@example.com")
                        .password("test1234")
                        .nickname("tester")
                        .rating(rating)
                        .mmr(mmr)
                        .deviceId("test-device")
                        .lastAccessedAt(Instant.now())
                        .deletedAt(CONST_FUTURE_INSTANT)
                        .status(Status.getDefaultStatus())
                        .build()
        );
    }


}
