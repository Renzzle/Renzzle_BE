package com.renzzle.backend.domain.puzzle.training.service;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.puzzle.rank.util.TrainingPuzzleSeeder;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.shared.util.RatingUtil;
import com.renzzle.backend.domain.puzzle.training.api.request.UpdateTrainingPackRequest;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

// One persistence context, so the pack save merges like in production
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
@Transactional
class TrainingPackRatingIntegrationTest {

    @Autowired private TrainingService trainingService;
    @Autowired private TrainingPuzzleSeeder trainingPuzzleSeeder;
    @Autowired private TrainingPuzzleRepository trainingPuzzleRepository;
    @Autowired private EntityManager em;

    @Test
    void updatePack_WhenDifficultyChanges_ThenUnrankedPuzzlesTakeTheNewDifficulty() {
        // given: the seeder puts the puzzle in a MIDDLE pack of its own
        trainingPuzzleSeeder.seedPuzzle(0, "h8h9", "h10", 7, 1000, "BLACK");
        TrainingPuzzle puzzle = trainingPuzzleRepository.findAll().stream()
                .filter(p -> p.getBoardStatus().equals("h8h9"))
                .findFirst().orElseThrow();

        // when
        trainingService.updatePack(puzzle.getPack().getId(),
                new UpdateTrainingPackRequest(Collections.emptyList(), 0, "HIGH"));
        em.flush();
        em.clear();

        // then
        assertThat(trainingPuzzleRepository.findById(puzzle.getId()).orElseThrow().getRating())
                .isEqualTo(RatingUtil.puzzleRating(7, WinColor.getWinColor("BLACK"), Difficulty.getDifficulty("HIGH")));
    }
}
