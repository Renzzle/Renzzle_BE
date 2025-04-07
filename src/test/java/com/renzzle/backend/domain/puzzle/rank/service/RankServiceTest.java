package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.puzzle.rank.dao.LatestRankPuzzleRepository;
import com.renzzle.backend.domain.puzzle.rank.domain.LatestRankPuzzle;
import com.renzzle.backend.domain.puzzle.rank.util.PuzzleSeeder;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.util.ELOUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
@Transactional
public class RankServiceTest {

    @Autowired private RankService rankService;
    @Autowired private TrainingPuzzleRepository trainingPuzzleRepository;
    @Autowired private LatestRankPuzzleRepository latestRankPuzzleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PuzzleSeeder puzzleSeeder;

    @PersistenceContext
    private EntityManager em;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(UserEntity.builder()
                .email("duplicate@test.com")
                .nickname("dup")
                .password("password")
                .rating(1500)
                .mmr(1500)
                .deviceId("dup-device")
                .deletedAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .lastAccessedAt(Instant.now())
                .status(Status.getDefaultStatus())
                .build());

        // 문제 등록
        puzzleSeeder.seedPuzzle(1, "a1a2", "a3",3,  1400, "BLACK");
        puzzleSeeder.seedPuzzle(2, "b1b2", "b3", 3, 1450, "WHITE");
        puzzleSeeder.seedPuzzle(3, "c1c2", "c3", 4, 1500, "BLACK");
        puzzleSeeder.seedPuzzle(4, "d1d2", "d3", 5, 1550, "WHITE");
        puzzleSeeder.seedPuzzle(5, "e1e2", "e3", 6, 1600, "BLACK");
        puzzleSeeder.seedPuzzle(6, "a1a2a3", "a13",3,  1353, "BLACK");

        em.flush();
        em.clear();
    }

    @Test
    void getNextPuzzle_ShouldReturnCorrectPuzzleAndAvoidDuplicates() {

        double targetWinProb = 0.7;
        TrainingPuzzle firstPuzzle = rankService.getNextPuzzle(user.getMmr(), targetWinProb, user);

        assertEquals(1353, firstPuzzle.getRating(), "첫 번째 문제는 1353이어야 함");

        // 저장 → 중복 방지를 위해
        latestRankPuzzleRepository.save(
                LatestRankPuzzle.builder()
                        .user(user)
                        .boardStatus(firstPuzzle.getBoardStatus())
                        .answer(firstPuzzle.getAnswer())
                        .isSolved(true)
                        .assignedAt(Instant.now())
                        .winColor(firstPuzzle.getWinColor())
                        .build()
        );

        em.flush();
        em.clear();

        double newMmr = user.getMmr() + ELOUtil.calculateMMRIncrease(user.getMmr(), firstPuzzle.getRating());
        user.updateMmrTo(newMmr);
        userRepository.save(user);

        em.flush();
        em.clear();

        TrainingPuzzle secondPuzzle = rankService.getNextPuzzle(user.getMmr(), targetWinProb - 0.05, user);

        assertNotEquals(firstPuzzle.getId(), secondPuzzle.getId(), "같은 문제 다시 출제되면 안 됨");
        assertEquals(1400, secondPuzzle.getRating(), "두 번째 문제는 1400이어야 함");
    }

}
