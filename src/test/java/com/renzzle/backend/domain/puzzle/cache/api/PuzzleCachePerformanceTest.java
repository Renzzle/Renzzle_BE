package com.renzzle.backend.domain.puzzle.cache.api;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.puzzle.cache.dao.PuzzleCacheRepository;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleCache;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.domain.puzzle.cache.domain.SolutionSerializer;
import com.renzzle.backend.domain.puzzle.shared.util.ZobristHashUtils;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.jpa.show-sql=false",
                "spring.jpa.properties.hibernate.show_sql=false",
                "spring.jpa.properties.hibernate.format_sql=false",
                "spring.jpa.properties.hibernate.use_sql_comments=false",
                "logging.level.org.hibernate.SQL=WARN",
                "logging.level.org.hibernate.orm.jdbc.bind=WARN"
        }
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
class PuzzleCachePerformanceTest {

    @Autowired
    private PuzzleCacheRepository puzzleCacheRepository;

    @Autowired
    private SolutionSerializer serializer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String TEST_BOARD_STATE = "h8h9i8i9";
    private static final PuzzleType TYPE = PuzzleType.COMMUNITY;
    private static final int MEASURE_ITERATIONS = 10;

    private HttpHeaders authHeaders;

    @BeforeEach
    void setUp() {
        puzzleCacheRepository.deleteAll();

        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        String token = jwtProvider.createAccessToken(user.getId());

        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
    }

    @Test
    void lookupPerformance_WhenCachePuzzleRowsGrowTo10000_ThenComparesLatency() {
        long knownHash = ZobristHashUtils.hashFromBoardStatus(TEST_BOARD_STATE);

        Map<Long, Integer> targetDag = new HashMap<>();
        targetDag.put(knownHash, 112);
        for (int j = 1; j < 10; j++) {
            targetDag.put(ThreadLocalRandom.current().nextLong(), j % 225);
        }
        puzzleCacheRepository.save(PuzzleCache.builder()
                .puzzleType(TYPE)
                .puzzleId(1L)
                .rootBoardState("h8")
                .solutionDag(serializer.serialize(targetDag))
                .build());

        double beforeMs = measureApiCall(1L);

        List<PuzzleCache> dummies = new ArrayList<>();
        for (long id = 2; id <= 10_000; id++) {
            Map<Long, Integer> dummyDag = new HashMap<>();
            for (int j = 0; j < 10; j++) {
                dummyDag.put(ThreadLocalRandom.current().nextLong(), j % 225);
            }
            dummies.add(PuzzleCache.builder()
                    .puzzleType(TYPE)
                    .puzzleId(id)
                    .rootBoardState("h8")
                    .solutionDag(serializer.serialize(dummyDag))
                    .build());
        }
        puzzleCacheRepository.saveAll(dummies);

        double afterMs = measureApiCall(1L);

        System.out.println("========================================");
        System.out.println("[Scenario 1] cache_puzzle row count growth");
        System.out.println("========================================");
        System.out.println("[Before] 1 puzzle       -> avg lookup: " + String.format("%.2f", beforeMs) + " ms");
        System.out.println("[After]  10,000 puzzles -> avg lookup: " + String.format("%.2f", afterMs) + " ms");
        System.out.println("[Delta] " + String.format("%.2f", afterMs - beforeMs) + " ms");
        System.out.println("========================================");

        assertThat(afterMs).isPositive();
    }

    @Test
    void lookupPerformance_WhenSolutionDagGrowsTo10000Entries_ThenComparesLatency() {
        long knownHash = ZobristHashUtils.hashFromBoardStatus(TEST_BOARD_STATE);

        Map<Long, Integer> smallDag = new HashMap<>();
        smallDag.put(knownHash, 112);
        for (int j = 1; j < 10; j++) {
            smallDag.put(ThreadLocalRandom.current().nextLong(), j % 225);
        }
        PuzzleCache puzzle = puzzleCacheRepository.save(PuzzleCache.builder()
                .puzzleType(TYPE)
                .puzzleId(1L)
                .rootBoardState("h8")
                .solutionDag(serializer.serialize(smallDag))
                .build());

        double beforeMs = measureApiCall(1L);

        Map<Long, Integer> largeDag = new HashMap<>();
        largeDag.put(knownHash, 112);
        for (int j = 1; j < 10_000; j++) {
            largeDag.put(ThreadLocalRandom.current().nextLong(), j % 225);
        }
        puzzleCacheRepository.save(puzzle.toBuilder()
                .solutionDag(serializer.serialize(largeDag))
                .build());

        double afterMs = measureApiCall(1L);

        System.out.println("========================================");
        System.out.println("[Scenario 2] solution_dag size growth");
        System.out.println("========================================");
        System.out.println("[Before] map of 10     -> avg lookup: " + String.format("%.2f", beforeMs) + " ms");
        System.out.println("[After]  map of 10,000 -> avg lookup: " + String.format("%.2f", afterMs) + " ms");
        System.out.println("[Delta] " + String.format("%.2f", afterMs - beforeMs) + " ms");
        System.out.println("========================================");

        assertThat(afterMs).isPositive();
    }

    private double measureApiCall(Long puzzleId) {
        String url = "/api/puzzle/cache/ai-response?puzzleType={type}&puzzleId={id}&currentBoardState={state}";
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders);

        // warmup
        restTemplate.exchange(url, HttpMethod.GET, entity, String.class, TYPE.name(), puzzleId, TEST_BOARD_STATE);

        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            stopWatch.start();
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class,
                    TYPE.name(), puzzleId, TEST_BOARD_STATE
            );
            stopWatch.stop();

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }

        return (stopWatch.getTotalTimeNanos() / (double) MEASURE_ITERATIONS) / 1_000_000.0;
    }
}
