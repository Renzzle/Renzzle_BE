package com.renzzle.backend.domain.puzzle.training.service;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.puzzle.rank.util.PackSeeder;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.shared.dto.AnswerKeyRecalculationResult;
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

// Keys are written by native updates, so the persistence context is cleared before reading them back
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
@Transactional
class TrainingAnswerKeyRecalculationIntegrationTest {

    private static final String BOARD = "h8i8h9i9h10i10";
    private static final String ANSWER = "h11h12h7";

    @Autowired private TrainingService trainingService;
    @Autowired private TrainingPuzzleRepository trainingPuzzleRepository;
    @Autowired private PackSeeder packSeeder;
    @Autowired private EntityManager em;

    @Test
    void recalculateAnswerKeys_WhenKeyIsMissing_ThenFillsIt() {
        TrainingPuzzle puzzle = savePuzzleWithoutAnswerKey(ANSWER);

        AnswerKeyRecalculationResult result = trainingService.recalculateAnswerKeys();
        em.clear();

        assertThat(result).isEqualTo(new AnswerKeyRecalculationResult(1, List.of()));
        assertThat(answerKey(puzzle)).isEqualTo(BoardUtils.makeAnswerKey(BOARD, ANSWER));
        assertThat(trainingService.recalculateAnswerKeys().updatedCount()).isZero();
    }

    @Test
    void recalculateAnswerKeys_WhenAnswerCannotBeParsed_ThenReportsItAndLeavesItWithoutKey() {
        TrainingPuzzle broken = savePuzzleWithoutAnswerKey("not a move");

        AnswerKeyRecalculationResult result = trainingService.recalculateAnswerKeys();
        em.clear();

        assertThat(result).isEqualTo(new AnswerKeyRecalculationResult(0, List.of(broken.getId())));
        assertThat(answerKey(broken)).isNull();
    }

    private TrainingPuzzle savePuzzleWithoutAnswerKey(String answer) {
        return trainingPuzzleRepository.save(TrainingPuzzle.builder()
                .pack(packSeeder.seedPack("LOW", 1, 0))
                .trainingIndex(0)
                .boardStatus(BOARD)
                .boardKey(BoardUtils.makeBoardKey(BOARD))
                .answer(answer)
                .depth(3)
                .rating(1000)
                .winColor(WinColor.getWinColor("BLACK"))
                .build());
    }

    private String answerKey(TrainingPuzzle puzzle) {
        return trainingPuzzleRepository.findById(puzzle.getId()).orElseThrow().getAnswerKey();
    }
}
