package com.renzzle.backend.domain.puzzle.rank.service;

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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RankServiceTest {
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

        em.flush();
        em.clear();
    }

    @Test
    void getNextPuzzle_ShouldNotReturnAlreadyGivenPuzzle() {
        // 1️⃣ 기존 문제 가져오기
        double targetWinProb = 0.7;
        double desiredRating = ELOUtil.getProblemRatingForTargetWinProbability(user.getMmr(), targetWinProb);
        double tolerance = 100;

        List<TrainingPuzzle> firstCandidates =
                trainingPuzzleRepository.findAvailablePuzzlesForUser(desiredRating - tolerance, desiredRating + tolerance, user);

        assertFalse(firstCandidates.isEmpty(), "첫 번째 후보 문제 존재해야 함");

        TrainingPuzzle firstPuzzle = firstCandidates.get(0);

        // 2️⃣ 문제 출제되었다고 저장
        latestRankPuzzleRepository.save(LatestRankPuzzle.builder()
                .user(user)
                .boardStatus(firstPuzzle.getBoardStatus())
                .answer(firstPuzzle.getAnswer())
                .isSolved(false)
                .assignedAt(Instant.now())
                .winColor(firstPuzzle.getWinColor())
                .build());

        em.flush();
        em.clear();

        // 3️⃣ 다시 getAvailable 호출 → 이전 문제 없어야 함
        List<TrainingPuzzle> secondCandidates =
                trainingPuzzleRepository.findAvailablePuzzlesForUser(desiredRating - tolerance, desiredRating + tolerance, user);

        boolean hasDuplicate = secondCandidates.stream()
                .anyMatch(p -> p.getId().equals(firstPuzzle.getId()));

        assertFalse(hasDuplicate, "이미 출제된 문제는 다시 출제되어서는 안 됨");
    }

}
