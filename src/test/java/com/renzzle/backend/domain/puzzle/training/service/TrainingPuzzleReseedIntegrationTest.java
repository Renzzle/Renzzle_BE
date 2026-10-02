package com.renzzle.backend.domain.puzzle.training.service;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.puzzle.cache.dao.PuzzleCacheRepository;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleCache;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.domain.puzzle.cache.domain.SolutionSerializer;
import com.renzzle.backend.domain.puzzle.rank.util.TrainingPuzzleSeeder;
import com.renzzle.backend.domain.puzzle.shared.util.ZobristHashUtils;
import com.renzzle.backend.domain.puzzle.training.api.request.ModifyTrainingPuzzleRequest;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// One persistence context, so the puzzle save merges into the loaded entity like in production
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
@Transactional
class TrainingPuzzleReseedIntegrationTest {

    private static final String BOARD = "h8h9";
    private static final String ANSWER = "i8i9j8";
    private static final long MANUAL_POSITION = ZobristHashUtils.hashFromBoardStatus("h8h9l5");

    @Autowired private TrainingService trainingService;
    @Autowired private TrainingPuzzleSeeder trainingPuzzleSeeder;
    @Autowired private TrainingPuzzleRepository trainingPuzzleRepository;
    @Autowired private PuzzleCacheRepository puzzleCacheRepository;
    @Autowired private SolutionSerializer solutionSerializer;
    @Autowired private EntityManager em;

    @Test
    void modifyTrainingPuzzle_WhenBoardAndAnswerUnchanged_ThenKeepsCachedReplies() {
        TrainingPuzzle puzzle = givenPuzzleWithManualCache();

        trainingService.modifyTrainingPuzzle(puzzle.getId(), request(puzzle, ANSWER));
        em.flush();
        em.clear();

        assertThat(cachedReplies(puzzle)).containsEntry(MANUAL_POSITION, 7);
    }

    @Test
    void modifyTrainingPuzzle_WhenAnswerChanges_ThenReseedsTheCache() {
        TrainingPuzzle puzzle = givenPuzzleWithManualCache();

        trainingService.modifyTrainingPuzzle(puzzle.getId(), request(puzzle, "i8i9j8j9k8"));
        em.flush();
        em.clear();

        assertThat(cachedReplies(puzzle)).hasSize(2).doesNotContainKey(MANUAL_POSITION);
    }

    private TrainingPuzzle givenPuzzleWithManualCache() {
        trainingPuzzleSeeder.seedPuzzle(0, BOARD, ANSWER, 3, 1000, "BLACK");
        TrainingPuzzle puzzle = trainingPuzzleRepository.findAll().stream()
                .filter(p -> p.getBoardStatus().equals(BOARD))
                .findFirst().orElseThrow();
        puzzleCacheRepository.save(PuzzleCache.builder()
                .puzzleType(PuzzleType.TRAINING)
                .puzzleId(puzzle.getId())
                .rootBoardState(BOARD)
                .solutionDag(solutionSerializer.serialize(Map.of(MANUAL_POSITION, 7)))
                .build());
        return puzzle;
    }

    private ModifyTrainingPuzzleRequest request(TrainingPuzzle puzzle, String answer) {
        return new ModifyTrainingPuzzleRequest(
                puzzle.getPack().getId(), null, BOARD, answer, puzzle.getDepth(), "BLACK");
    }

    private Map<Long, Integer> cachedReplies(TrainingPuzzle puzzle) {
        PuzzleCache cache = puzzleCacheRepository
                .findByPuzzleTypeAndPuzzleId(PuzzleType.TRAINING, puzzle.getId())
                .orElseThrow();
        return solutionSerializer.deserialize(cache.getSolutionDag());
    }
}
