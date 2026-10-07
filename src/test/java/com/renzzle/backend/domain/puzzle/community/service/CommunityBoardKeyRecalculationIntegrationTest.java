package com.renzzle.backend.domain.puzzle.community.service;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.shared.dto.BoardKeyRecalculationResult;
import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.support.TestCommunityPuzzleBuilder;
import com.renzzle.backend.support.TestUserEntityBuilder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.renzzle.backend.support.TestTime.FIXED_INSTANT;
import static org.assertj.core.api.Assertions.assertThat;

// Keys are rewritten by native updates, so the persistence context is cleared before reading them back
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
@Transactional
class CommunityBoardKeyRecalculationIntegrationTest {

    private static final String BOARD = "h8h10i9";
    private static final String ROTATED_BOARD = "h8j8i7";
    private static final String OTHER_BOARD = "a1a4b8";

    @Autowired private CommunityService communityService;
    @Autowired private CommunityPuzzleRepository communityPuzzleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    private UserEntity author;

    @BeforeEach
    void setUp() {
        author = TestUserEntityBuilder.builder().save(userRepository);
    }

    @Test
    void recalculateBoardKeys_WhenKeysAreStale_ThenRewritesLiveAndDeletedPuzzles() {
        CommunityPuzzle live = savePuzzle(BOARD, "stale-live");
        CommunityPuzzle deleted = savePuzzle(OTHER_BOARD, "stale-deleted");
        communityPuzzleRepository.softDelete(deleted.getId(), FIXED_INSTANT);

        BoardKeyRecalculationResult result = communityService.recalculateBoardKeys();
        em.clear();

        assertThat(result).isEqualTo(new BoardKeyRecalculationResult(2, List.of()));
        assertThat(boardKey(live)).isEqualTo(BoardUtils.makeBoardKey(BOARD));
        assertThat(boardKey(deleted)).isEqualTo(BoardUtils.makeBoardKey(OTHER_BOARD));
        assertThat(communityService.recalculateBoardKeys().updatedCount()).isZero();
    }

    @Test
    void recalculateBoardKeys_WhenLivePuzzlesAreOnePosition_ThenReportsThemAndKeepsTheirKeys() {
        // A rotated copy saved with the current formula while the original still holds an old key
        CommunityPuzzle original = savePuzzle(BOARD, "stale-key");
        CommunityPuzzle copy = savePuzzle(ROTATED_BOARD, BoardUtils.makeBoardKey(ROTATED_BOARD));

        BoardKeyRecalculationResult result = communityService.recalculateBoardKeys();
        em.clear();

        assertThat(result.updatedCount()).isZero();
        assertThat(result.duplicates()).hasSize(1);
        assertThat(result.duplicates().get(0)).containsExactlyInAnyOrder(original.getId(), copy.getId());
        assertThat(boardKey(original)).isEqualTo("stale-key");
    }

    @Test
    void recalculateBoardKeys_WhenADeletedPuzzleRepeatsALivePosition_ThenRewritesBoth() {
        // deleted_at is part of the unique key, so the deleted copy never collides with the live one
        CommunityPuzzle live = savePuzzle(BOARD, "stale-live");
        CommunityPuzzle deleted = savePuzzle(ROTATED_BOARD, "stale-deleted");
        communityPuzzleRepository.softDelete(deleted.getId(), FIXED_INSTANT);

        BoardKeyRecalculationResult result = communityService.recalculateBoardKeys();
        em.clear();

        assertThat(result).isEqualTo(new BoardKeyRecalculationResult(2, List.of()));
        assertThat(boardKey(live)).isEqualTo(BoardUtils.makeBoardKey(BOARD)).isEqualTo(boardKey(deleted));
    }

    private CommunityPuzzle savePuzzle(String boardStatus, String boardKey) {
        return TestCommunityPuzzleBuilder.builder(author)
                .withBoardStatus(boardStatus)
                .withBoardKey(boardKey)
                .save(communityPuzzleRepository);
    }

    private String boardKey(CommunityPuzzle puzzle) {
        return communityPuzzleRepository.findByIdIncludingDeleted(puzzle.getId()).getBoardKey();
    }
}
