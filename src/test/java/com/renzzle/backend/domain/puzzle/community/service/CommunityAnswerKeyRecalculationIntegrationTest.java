package com.renzzle.backend.domain.puzzle.community.service;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.shared.dto.AnswerKeyRecalculationResult;
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

// Keys are written by native updates, so the persistence context is cleared before reading them back
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
@Transactional
class CommunityAnswerKeyRecalculationIntegrationTest {

    private static final String BOARD = "h8i8h9i9h10i10";
    private static final String ANSWER = "h11h12h7";

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
    void recalculateAnswerKeys_WhenKeyIsMissing_ThenFillsLivePuzzlesOnly() {
        CommunityPuzzle live = savePuzzleWithoutAnswerKey(BOARD, ANSWER);
        CommunityPuzzle deleted = savePuzzleWithoutAnswerKey("a1a2", "a3");
        communityPuzzleRepository.softDelete(deleted.getId(), FIXED_INSTANT);

        AnswerKeyRecalculationResult result = communityService.recalculateAnswerKeys();
        em.clear();

        assertThat(result).isEqualTo(new AnswerKeyRecalculationResult(1, List.of()));
        assertThat(answerKey(live)).isEqualTo(BoardUtils.makeAnswerKey(BOARD, ANSWER));
        assertThat(answerKey(deleted)).isNull();
        assertThat(communityService.recalculateAnswerKeys().updatedCount()).isZero();
    }

    @Test
    void recalculateAnswerKeys_WhenAnswerCannotBeParsed_ThenReportsItAndLeavesItWithoutKey() {
        CommunityPuzzle broken = savePuzzleWithoutAnswerKey(BOARD, "not a move");

        AnswerKeyRecalculationResult result = communityService.recalculateAnswerKeys();
        em.clear();

        assertThat(result).isEqualTo(new AnswerKeyRecalculationResult(0, List.of(broken.getId())));
        assertThat(answerKey(broken)).isNull();
    }

    private CommunityPuzzle savePuzzleWithoutAnswerKey(String boardStatus, String answer) {
        return TestCommunityPuzzleBuilder.builder(author)
                .withBoardStatus(boardStatus)
                .withAnswer(answer)
                .save(communityPuzzleRepository);
    }

    private String answerKey(CommunityPuzzle puzzle) {
        return communityPuzzleRepository.findByIdIncludingDeleted(puzzle.getId()).getAnswerKey();
    }
}
