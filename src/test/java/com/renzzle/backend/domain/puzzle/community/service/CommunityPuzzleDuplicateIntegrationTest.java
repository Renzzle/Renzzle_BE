package com.renzzle.backend.domain.puzzle.community.service;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.puzzle.community.api.request.AddCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.rank.util.PackSeeder;
import com.renzzle.backend.domain.puzzle.rank.util.TrainingPuzzleSeeder;
import com.renzzle.backend.domain.puzzle.training.api.request.AddTrainingPuzzleRequest;
import com.renzzle.backend.domain.puzzle.training.service.TrainingService;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static com.renzzle.backend.support.TestTime.FIXED_INSTANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Runs against MySQL so the existence checks see the real soft-delete restriction
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class CommunityPuzzleDuplicateIntegrationTest {

    private static final String BOARD = "h8h10i9";
    private static final String ROTATED_BOARD = "h8j8i7";
    // Black h8-h10 against white i8-i10; black h11, white h12, black h7 makes five
    private static final String PUZZLE_BOARD = "h8i8h9i9h10i10";
    private static final String PUZZLE_ANSWER = "h11h12h7";

    @Autowired private CommunityService communityService;
    @Autowired private CommunityPuzzleRepository communityPuzzleRepository;
    @Autowired private TrainingService trainingService;
    @Autowired private TrainingPuzzleSeeder trainingPuzzleSeeder;
    @Autowired private PackSeeder packSeeder;
    @Autowired private UserRepository userRepository;

    private UserEntity author;

    @BeforeEach
    void setUp() {
        author = TestUserEntityBuilder.builder().save(userRepository);
    }

    @Test
    void addCommunityPuzzle_WhenRotatedCopyOfTrainingPuzzle_ThenRejectsAsDuplicate() {
        trainingPuzzleSeeder.seedPuzzle(0, BOARD, "h11", 3, 1000, "BLACK");

        CustomException exception = assertThrows(CustomException.class, () -> upload(ROTATED_BOARD));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_PUZZLE);
    }

    @Test
    void addCommunityPuzzle_WhenRotatedCopyOfLiveCommunityPuzzle_ThenRejectsAsDuplicate() {
        upload(BOARD);

        CustomException exception = assertThrows(CustomException.class, () -> upload(ROTATED_BOARD));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_PUZZLE);
    }

    @Test
    void addCommunityPuzzle_WhenSamePositionWasDeleted_ThenAcceptsIt() {
        communityPuzzleRepository.softDelete(upload(BOARD), FIXED_INSTANT);

        assertThatNoException().isThrownBy(() -> upload(BOARD));
    }

    @Test
    void addCommunityPuzzle_WhenTrainingPuzzleIsPaddedWithStones_ThenRejectsAsDuplicate() {
        Long packId = packSeeder.seedPack("LOW", 0, 0).getId();
        trainingService.createTrainingPuzzle(
                new AddTrainingPuzzleRequest(packId, 0, PUZZLE_BOARD, PUZZLE_ANSWER, 3, "BLACK"));

        CustomException exception = assertThrows(CustomException.class,
                () -> upload(PUZZLE_BOARD + "a1o15", PUZZLE_ANSWER));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_PUZZLE);
    }

    @Test
    void addCommunityPuzzle_WhenCommunityPuzzleIsTurnedShiftedAndPadded_ThenRejectsAsDuplicate() {
        upload(PUZZLE_BOARD, PUZZLE_ANSWER);

        // A quarter turn that puts the first answer move on e5, plus a far pair of stones
        CustomException exception = assertThrows(CustomException.class,
                () -> upload("b5b4c5c4d5d4o1o15", "e5f5a5"));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_PUZZLE);
    }

    @Test
    void addCommunityPuzzle_WhenStonesAreRemovedFromCommunityPuzzle_ThenRejectsAsDuplicate() {
        upload(PUZZLE_BOARD + "f8g8f9g9", PUZZLE_ANSWER);

        // Eight of the original ten stones
        CustomException exception = assertThrows(CustomException.class,
                () -> upload(PUZZLE_BOARD + "f9g9", PUZZLE_ANSWER));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_PUZZLE);
    }

    @Test
    void addCommunityPuzzle_WhenRejectedAsDuplicate_ThenLogsTheMatchedPuzzle(CapturedOutput output) {
        Long originalId = upload(PUZZLE_BOARD, PUZZLE_ANSWER);

        assertThrows(CustomException.class, () -> upload(PUZZLE_BOARD + "a1o15", PUZZLE_ANSWER));

        assertThat(output).contains("reason=near copy, matchedType=COMMUNITY, matchedId=" + originalId
                + ", boardStatus=" + PUZZLE_BOARD + "a1o15");
    }

    @Test
    void addCommunityPuzzle_WhenOneStoneOfCommunityPuzzleIsMoved_ThenRejectsAsDuplicate() {
        upload(PUZZLE_BOARD, PUZZLE_ANSWER);

        // White i10 moved to i11, so neither board holds the other
        CustomException exception = assertThrows(CustomException.class,
                () -> upload("h8i8h9i9h10i11", PUZZLE_ANSWER));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_PUZZLE);
    }

    @Test
    void addCommunityPuzzle_WhenStonesAreAddedOnlyBeyondReachOfAnswer_ThenRejectsAsDuplicate() {
        upload(PUZZLE_BOARD, PUZZLE_ANSWER);

        // Eight stones far from the answer
        CustomException exception = assertThrows(CustomException.class,
                () -> upload(PUZZLE_BOARD + "a1o15a15o1b1n15b15n1", PUZZLE_ANSWER));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_PUZZLE);
    }

    @Test
    void addCommunityPuzzle_WhenManyStonesAreAddedNearAnswer_ThenAcceptsAndLogsTheOriginal(CapturedOutput output) {
        Long originalId = upload(PUZZLE_BOARD, PUZZLE_ANSWER);

        // Six stones near the answer
        Long puzzleId = upload(PUZZLE_BOARD + "j11k11j12k12j13k13", PUZZLE_ANSWER);

        assertThat(output).contains("puzzleId=" + puzzleId + ", matchedType=COMMUNITY, matchedId=" + originalId);
    }

    private Long upload(String boardStatus) {
        return upload(boardStatus, "h11");
    }

    private Long upload(String boardStatus, String answer) {
        return communityService.addCommunityPuzzle(
                new AddCommunityPuzzleRequest(boardStatus, answer, 3, null, "BLACK", false), author).puzzleId();
    }
}
