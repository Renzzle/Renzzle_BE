package com.renzzle.backend.domain.puzzle.community.service;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.puzzle.community.api.request.AddCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.rank.util.TrainingPuzzleSeeder;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class CommunityPuzzleDuplicateIntegrationTest {

    private static final String BOARD = "h8h10i9";
    private static final String ROTATED_BOARD = "h8j8i7";

    @Autowired private CommunityService communityService;
    @Autowired private CommunityPuzzleRepository communityPuzzleRepository;
    @Autowired private TrainingPuzzleSeeder trainingPuzzleSeeder;
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

    private Long upload(String boardStatus) {
        return communityService.addCommunityPuzzle(
                new AddCommunityPuzzleRequest(boardStatus, "h11", 3, null, "BLACK", false), author).puzzleId();
    }
}
