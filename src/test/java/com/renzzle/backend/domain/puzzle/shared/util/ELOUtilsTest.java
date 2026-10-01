package com.renzzle.backend.domain.puzzle.shared.util;

import org.junit.jupiter.api.Test;

import static com.renzzle.backend.domain.puzzle.shared.util.ELOUtils.MAX_TARGET_WIN_PROBABILITY;
import static com.renzzle.backend.domain.puzzle.shared.util.ELOUtils.MIN_TARGET_WIN_PROBABILITY;
import static com.renzzle.backend.domain.puzzle.shared.util.ELOUtils.TARGET_WIN_PROBABILITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ELOUtilsTest {

    private static final double EPS = 1e-9;
    private static final double USER_MMR = 1000.0;

    @Test
    void nextTargetWinProbability_WhenSolvedInRange_ThenStepsDownByDelta() {
        assertEquals(0.65, ELOUtils.nextTargetWinProbability(0.7, true), EPS);
    }

    @Test
    void nextTargetWinProbability_WhenMissedInRange_ThenStepsUpByDelta() {
        assertEquals(0.75, ELOUtils.nextTargetWinProbability(0.7, false), EPS);
    }

    @Test
    void nextTargetWinProbability_WhenSolveStreakRunsLong_ThenStopsAtMin() {
        double p = TARGET_WIN_PROBABILITY;
        for (int i = 0; i < 30; i++) {
            p = ELOUtils.nextTargetWinProbability(p, true);
        }
        assertEquals(MIN_TARGET_WIN_PROBABILITY, p, EPS);
    }

    @Test
    void nextTargetWinProbability_WhenMissStreakRunsLong_ThenStopsAtMax() {
        double p = TARGET_WIN_PROBABILITY;
        for (int i = 0; i < 30; i++) {
            p = ELOUtils.nextTargetWinProbability(p, false);
        }
        assertEquals(MAX_TARGET_WIN_PROBABILITY, p, EPS);
    }

    @Test
    void nextTargetWinProbability_WhenStreakBreaksAtBound_ThenMovesBackRightAway() {
        assertEquals(0.10, ELOUtils.nextTargetWinProbability(MIN_TARGET_WIN_PROBABILITY, false), EPS);
        assertEquals(0.90, ELOUtils.nextTargetWinProbability(MAX_TARGET_WIN_PROBABILITY, true), EPS);
    }

    @Test
    void nextTargetWinProbability_WhenStoredValueAlreadyOutOfRange_ThenPullsBackIntoRange() {
        // assignment rows written before the clamp existed can hold values past either bound
        assertEquals(MIN_TARGET_WIN_PROBABILITY, ELOUtils.nextTargetWinProbability(-0.3, true), EPS);
        assertEquals(MIN_TARGET_WIN_PROBABILITY, ELOUtils.nextTargetWinProbability(-0.3, false), EPS);
        assertEquals(MAX_TARGET_WIN_PROBABILITY, ELOUtils.nextTargetWinProbability(1.4, false), EPS);
        assertEquals(MAX_TARGET_WIN_PROBABILITY, ELOUtils.nextTargetWinProbability(1.4, true), EPS);
    }

    @Test
    void getProblemRatingForTargetWinProbability_WhenSolveStreakRunsLong_ThenNeverGetsEasier() {
        // unclamped, the 14th straight solve pushed p below 0 and the next puzzle fell back to the user's own rating
        double p = TARGET_WIN_PROBABILITY;
        double previous = ELOUtils.getProblemRatingForTargetWinProbability(USER_MMR, p);
        for (int i = 0; i < 30; i++) {
            p = ELOUtils.nextTargetWinProbability(p, true);
            double desired = ELOUtils.getProblemRatingForTargetWinProbability(USER_MMR, p);
            assertThat(desired).isFinite().isGreaterThanOrEqualTo(previous);
            previous = desired;
        }
    }

    @Test
    void getProblemRatingForTargetWinProbability_WhenMissStreakRunsLong_ThenNeverGetsHarder() {
        // unclamped, the 6th straight miss pushed p above 1 and the next puzzle jumped back to the user's own rating
        double p = TARGET_WIN_PROBABILITY;
        double previous = ELOUtils.getProblemRatingForTargetWinProbability(USER_MMR, p);
        for (int i = 0; i < 30; i++) {
            p = ELOUtils.nextTargetWinProbability(p, false);
            double desired = ELOUtils.getProblemRatingForTargetWinProbability(USER_MMR, p);
            assertThat(desired).isFinite().isLessThanOrEqualTo(previous);
            previous = desired;
        }
    }

    @Test
    void puzzleKFactor_WhenNoResultsYet_ThenStartsAtMax() {
        assertEquals(40.0, ELOUtils.puzzleKFactor(0), EPS);
    }

    @Test
    void puzzleKFactor_WhenResultsAccumulate_ThenShrinksDownToMin() {
        assertEquals(20.0, ELOUtils.puzzleKFactor(10), EPS);

        double previous = ELOUtils.puzzleKFactor(0);
        for (int attempts = 1; attempts <= 200; attempts++) {
            double k = ELOUtils.puzzleKFactor(attempts);
            assertThat(k).isLessThanOrEqualTo(previous).isGreaterThanOrEqualTo(8.0);
            previous = k;
        }
        assertEquals(8.0, ELOUtils.puzzleKFactor(200), EPS);
    }

    @Test
    void calculatePuzzleRatingChange_WhenEvenlyMatched_ThenMovesByHalfOfK() {
        assertEquals(-20.0, ELOUtils.calculatePuzzleRatingChange(1000, 1000, 0, true), EPS);
        assertEquals(20.0, ELOUtils.calculatePuzzleRatingChange(1000, 1000, 0, false), EPS);
    }

    @Test
    void calculatePuzzleRatingChange_WhenEasyPuzzleMissed_ThenRisesMoreThanHardPuzzleMissed() {
        // missing a puzzle the user was expected to solve says more about the puzzle
        double easyMissed = ELOUtils.calculatePuzzleRatingChange(1500, 1200, 0, false);
        double hardMissed = ELOUtils.calculatePuzzleRatingChange(1500, 1800, 0, false);

        assertThat(hardMissed).isPositive();
        assertThat(easyMissed).isGreaterThan(hardMissed);
    }

    @Test
    void calculatePuzzleRatingChange_WhenHardPuzzleSolved_ThenDropsMoreThanEasyPuzzleSolved() {
        double hardSolved = ELOUtils.calculatePuzzleRatingChange(1500, 1800, 0, true);
        double easySolved = ELOUtils.calculatePuzzleRatingChange(1500, 1200, 0, true);

        assertThat(easySolved).isNegative();
        assertThat(hardSolved).isLessThan(easySolved);
    }

    @Test
    void calculatePuzzleRatingChange_WhenPuzzleWellAttempted_ThenMovesLess() {
        double fresh = ELOUtils.calculatePuzzleRatingChange(1000, 1000, 0, false);
        double settled = ELOUtils.calculatePuzzleRatingChange(1000, 1000, 100, false);

        assertThat(settled).isPositive().isLessThan(fresh);
    }
}
