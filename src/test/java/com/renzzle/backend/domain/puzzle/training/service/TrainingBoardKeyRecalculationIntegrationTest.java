package com.renzzle.backend.domain.puzzle.training.service;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.puzzle.rank.util.PackSeeder;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.shared.dto.BoardKeyRecalculationResult;
import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Keys are rewritten by native updates, so the persistence context is cleared before reading them back
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
@Transactional
class TrainingBoardKeyRecalculationIntegrationTest {

    private static final String BOARD = "h8h10i9";
    private static final String ROTATED_BOARD = "h8j8i7";
    private static final String OTHER_BOARD = "a1a4b8";

    @Autowired private TrainingService trainingService;
    @Autowired private TrainingPuzzleRepository trainingPuzzleRepository;
    @Autowired private PackSeeder packSeeder;
    @Autowired private EntityManager em;

    @Test
    void recalculateBoardKeys_WhenKeyIsStale_ThenRewritesOnlyThatKey() {
        TrainingPuzzle stale = savePuzzle(BOARD, "stale-key");
        TrainingPuzzle current = savePuzzle(OTHER_BOARD, BoardUtils.makeBoardKey(OTHER_BOARD));

        BoardKeyRecalculationResult result = trainingService.recalculateBoardKeys();
        em.clear();

        assertThat(result).isEqualTo(new BoardKeyRecalculationResult(1, List.of()));
        assertThat(boardKey(stale)).isEqualTo(BoardUtils.makeBoardKey(BOARD));
        assertThat(boardKey(current)).isEqualTo(BoardUtils.makeBoardKey(OTHER_BOARD));
        assertThat(trainingService.recalculateBoardKeys().updatedCount()).isZero();
    }

    @Test
    void recalculateBoardKeys_WhenTwoPuzzlesAreOnePosition_ThenReportsThemAndKeepsTheirKeys() {
        // A rotated copy saved with the current formula while the original still holds an old key
        TrainingPuzzle original = savePuzzle(BOARD, "stale-key");
        TrainingPuzzle copy = savePuzzle(ROTATED_BOARD, BoardUtils.makeBoardKey(ROTATED_BOARD));

        BoardKeyRecalculationResult result = trainingService.recalculateBoardKeys();
        em.clear();

        assertThat(result.updatedCount()).isZero();
        assertThat(result.duplicates()).hasSize(1);
        assertThat(result.duplicates().get(0)).containsExactlyInAnyOrder(original.getId(), copy.getId());
        assertThat(boardKey(original)).isEqualTo("stale-key");
    }

    private TrainingPuzzle savePuzzle(String boardStatus, String boardKey) {
        return trainingPuzzleRepository.save(TrainingPuzzle.builder()
                .pack(packSeeder.seedPack("LOW", 1, 0))
                .trainingIndex(0)
                .boardStatus(boardStatus)
                .boardKey(boardKey)
                .answer("h11")
                .depth(3)
                .rating(1000)
                .winColor(WinColor.getWinColor("BLACK"))
                .build());
    }

    private String boardKey(TrainingPuzzle puzzle) {
        return trainingPuzzleRepository.findById(puzzle.getId()).orElseThrow().getBoardKey();
    }
}
