package com.renzzle.backend.domain.puzzle.content.service;

import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzlesResponse;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.content.api.request.GetRecommendRequest;
import com.renzzle.backend.domain.puzzle.content.api.response.GetTrendPuzzlesResponse;
import com.renzzle.backend.domain.puzzle.content.api.response.GetRecommendPackResponse;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils;
import com.renzzle.backend.domain.puzzle.training.dao.PackRepository;
import com.renzzle.backend.domain.puzzle.training.dao.PackTranslationRepository;
import com.renzzle.backend.domain.puzzle.training.dao.SolvedTrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.dao.UserPackRepository;
import com.renzzle.backend.domain.puzzle.training.domain.*;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.support.TestCommunityPuzzleBuilder;
import com.renzzle.backend.support.TestPackBuilder;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {
    @Mock
    private SolvedTrainingPuzzleRepository solvedTrainingPuzzleRepository;

    @Mock
    private PackRepository packRepository;

    @Mock
    private PackTranslationRepository packTranslationRepository;

    @Mock
    private UserPackRepository userPackRepository;

    @Mock
    private CommunityPuzzleRepository communityPuzzleRepository;

    @Mock
    private UserCommunityPuzzleRepository userCommunityPuzzleRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private ContentService contentService;

    private UserEntity user;

    @BeforeEach
     void setup() {
        user = TestUserEntityBuilder.builder()
                .withId(1L)
                .withStatus(Status.getDefaultStatus())
                .build();
    }

    @Test
    void getRecommendedPack_WhenSolvedPuzzleExists_ThenReturnsRecommendedPack() {
        // Given
        Pack pack = TestPackBuilder.builder()
                .withId(1L)
                .withPuzzleCount(10)
                .withPrice(500)
                .build();

        TrainingPuzzle trainingPuzzle = TrainingPuzzle.builder()
                .pack(pack)
                .trainingIndex(1)
                .boardStatus("a1a2a3")
                .boardKey(BoardUtils.makeBoardKey("a1a2a3"))
                .answer("answer")
                .depth(1)
                .rating(1000.0)
                .winColor(WinColor.getWinColor("WHITE"))
                .build();

        SolvedTrainingPuzzle solvedTrainingPuzzle = SolvedTrainingPuzzle.builder()
                .user(user)
                .puzzle(trainingPuzzle)
                .build();

        PackTranslation translation = PackTranslation.builder()
                .pack(pack)
                .langCode(LangCode.getLangCode("EN"))
                .title("Default Title")
                .author("Default Author")
                .description("Default Description")
                .build();

        UserPack userPack = UserPack.builder()
                .user(user)
                .pack(pack)
                .solvedCount(5)
                .build();

        when(solvedTrainingPuzzleRepository.findTopByUserOrderBySolvedAtDesc(user.getId()))
                .thenReturn(Optional.of(solvedTrainingPuzzle));
        when(packTranslationRepository.findByPackAndLangCode(eq(pack), any()))
                .thenReturn(Optional.of(translation));
        when(userPackRepository.findByUserIdAndPackId(user.getId(), pack.getId()))
                .thenReturn(Optional.of(userPack));

        // When
        GetRecommendPackResponse response = contentService.getRecommendedPack(new GetRecommendRequest("EN"), user);

        // Then
        assertThat(response.id()).isEqualTo(pack.getId());
        assertThat(response.title()).isEqualTo(translation.getTitle());
        assertThat(response.solvedPuzzleCount()).isEqualTo(5);
        assertThat(response.locked()).isFalse();
    }

    @Test
    void getRecommendedPack_WhenNoSolvedPuzzle_ThenReturnsDefaultPack() {
        // Given
        Pack pack = TestPackBuilder.builder()
                .withId(1L)
                .build();

        PackTranslation translation = PackTranslation.builder()
                .pack(pack)
                .langCode(LangCode.getLangCode("EN"))
                .title("Default Title")
                .author("Default Author")
                .description("Default Description")
                .build();

        when(solvedTrainingPuzzleRepository.findTopByUserOrderBySolvedAtDesc(user.getId()))
                .thenReturn(Optional.empty());

        when(packRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(pack));
        when(packTranslationRepository.findByPackAndLangCode(eq(pack), any()))
                .thenReturn(Optional.of(translation));

        // When
        GetRecommendPackResponse response = contentService.getRecommendedPack(new GetRecommendRequest("EN"), user);

        // Then
        assertThat(response.id()).isEqualTo(pack.getId());
        assertThat(response.title()).isEqualTo(translation.getTitle());
        assertThat(response.solvedPuzzleCount()).isZero();
        assertThat(response.locked()).isFalse();
    }

    @Test
    void getRecommendedPack_WhenNoPackExists_ThenThrowsNoSuchTrainingPackException() {
        // Given
        when(solvedTrainingPuzzleRepository.findTopByUserOrderBySolvedAtDesc(user.getId()))
                .thenReturn(Optional.empty());

        when(packRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> contentService.getRecommendedPack(new GetRecommendRequest("EN"), user))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.NO_SUCH_TRAINING_PACK.getMessage());
    }

    @Test
    void getRecommendedPack_WhenNoPackTranslationExists_ThenThrowsNoSuchPackTranslationException() {
        // Given
        Pack pack = TestPackBuilder.builder()
                .withId(1L)
                .build();

        when(solvedTrainingPuzzleRepository.findTopByUserOrderBySolvedAtDesc(user.getId()))
                .thenReturn(Optional.empty());

        when(packRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(pack));

        when(packTranslationRepository.findByPackAndLangCode(eq(pack), any()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> contentService.getRecommendedPack(new GetRecommendRequest("EN"), user))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.NO_SUCH_PACK_TRANSLATION.getMessage());
    }

    @Test
    void getRecommendedPack_WhenNoUserPackExists_ThenThrowsNoUserProgressForPackException() {
        // Given
        Pack pack = TestPackBuilder.builder()
                .withId(1L)
                .build();

        TrainingPuzzle trainingPuzzle = TrainingPuzzle.builder()
                .pack(pack)
                .trainingIndex(1)
                .boardStatus("a1a2a3")
                .boardKey(BoardUtils.makeBoardKey("a1a2a3"))
                .answer("answer")
                .depth(1)
                .rating(1000.0)
                .winColor(WinColor.getWinColor("WHITE"))
                .build();

        SolvedTrainingPuzzle solvedTrainingPuzzle = SolvedTrainingPuzzle.builder()
                .user(user)
                .puzzle(trainingPuzzle)
                .build();

        PackTranslation translation = PackTranslation.builder()
                .pack(pack)
                .langCode(LangCode.getLangCode("EN"))
                .title("Default Title")
                .author("Default Author")
                .description("Default Description")
                .build();

        when(solvedTrainingPuzzleRepository.findTopByUserOrderBySolvedAtDesc(user.getId()))
                .thenReturn(Optional.of(solvedTrainingPuzzle));
        when(packTranslationRepository.findByPackAndLangCode(eq(pack), any()))
                .thenReturn(Optional.of(translation));
        when(userPackRepository.findByUserIdAndPackId(user.getId(), pack.getId()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> contentService.getRecommendedPack(new GetRecommendRequest("EN"), user))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.NO_USER_PROGRESS_FOR_PACK.getMessage());
    }

    @Test
    void getTrendCommunityPuzzles_WhenWeekFillsTheList_ThenSkipsOlderPuzzles() {
        // Given
        Instant now = Instant.parse("2024-04-28T00:00:00Z");
        when(clock.instant()).thenReturn(now);

        List<CommunityPuzzle> week = new ArrayList<>();
        for (long id = 1; id <= 5; id++) {
            week.add(TestCommunityPuzzleBuilder.builder(user).withId(id).withCreatedAt(now).build());
        }
        // A week back, likes halving every two days
        when(communityPuzzleRepository.findTrendPuzzlesSince(
                now.minus(7, ChronoUnit.DAYS), now, Duration.ofDays(2).toSeconds(), 5))
                .thenReturn(week);

        // When
        GetTrendPuzzlesResponse response = contentService.getTrendCommunityPuzzles(user);

        // Then: kept in the order the database ranked them
        assertThat(response.puzzles()).extracting(GetCommunityPuzzlesResponse::id)
                .containsExactly(1L, 2L, 3L, 4L, 5L);
        verify(communityPuzzleRepository, never()).findOlderTrendPuzzles(any(), any(), anyLong(), anyInt());
    }

    @Test
    void getTrendCommunityPuzzles_WhenWeekIsQuiet_ThenFillsTheRestFromOlderPuzzles() {
        // Given: only two puzzles qualify this week, and one older one
        Instant now = Instant.parse("2024-04-28T00:00:00Z");
        when(clock.instant()).thenReturn(now);

        when(communityPuzzleRepository.findTrendPuzzlesSince(any(), any(), anyLong(), eq(5)))
                .thenReturn(List.of(
                        TestCommunityPuzzleBuilder.builder(user).withId(1L).withCreatedAt(now).build(),
                        TestCommunityPuzzleBuilder.builder(user).withId(2L).withCreatedAt(now).build()));
        when(communityPuzzleRepository.findOlderTrendPuzzles(any(), any(), anyLong(), eq(3)))
                .thenReturn(List.of(
                        TestCommunityPuzzleBuilder.builder(user).withId(3L)
                                .withCreatedAt(now.minus(10, ChronoUnit.DAYS)).build()));

        // When
        GetTrendPuzzlesResponse response = contentService.getTrendCommunityPuzzles(user);

        // Then: fewer than five when no more qualify
        assertThat(response.puzzles()).extracting(GetCommunityPuzzlesResponse::id)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void getTrendCommunityPuzzles_WhenPuzzleSelected_ThenResponseFieldsMappedCorrectly() {
        // Given
        Instant now = Instant.parse("2024-04-28T00:00:00Z");
        when(clock.instant()).thenReturn(now);

        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user)
                .withId(1L)
                .withBoardStatus("some-fen")
                .withCreatedAt(now)
                .withLikeCount(10)
                .withDislikeCount(2)
                .withView(300)
                .withDepth(5)
                .withVerified(true)
                .withColor(WinColor.getWinColor("BLACK"))
                .build();

        when(communityPuzzleRepository.findTrendPuzzlesSince(any(), any(), anyLong(), anyInt()))
                .thenReturn(List.of(puzzle));
        when(communityPuzzleRepository.findOlderTrendPuzzles(any(), any(), anyLong(), anyInt()))
                .thenReturn(List.of());
        when(userCommunityPuzzleRepository.checkIsSolvedPuzzle(user.getId(), puzzle.getId()))
                .thenReturn(true);

        // When
        GetTrendPuzzlesResponse response = contentService.getTrendCommunityPuzzles(user);

        // Then
        GetCommunityPuzzlesResponse dto = response.puzzles().get(0);

        assertThat(dto.id()).isEqualTo(puzzle.getId());
        assertThat(dto.boardStatus()).isEqualTo(puzzle.getBoardStatus());
        assertThat(dto.authorId()).isEqualTo(user.getId());
        assertThat(dto.authorName()).isEqualTo(user.getNickname());
        assertThat(dto.depth()).isEqualTo(puzzle.getDepth());
        assertThat(dto.winColor()).isEqualTo(puzzle.getWinColor().getName());
        assertThat(dto.likeCount()).isEqualTo(puzzle.getLikeCount());
        assertThat(dto.views()).isEqualTo(puzzle.getView());
        assertThat(dto.createdAt()).isEqualTo(puzzle.getCreatedAt().toString());
        assertThat(dto.isSolved()).isTrue();
        assertThat(dto.isVerified()).isEqualTo(puzzle.getIsVerified());
        assertThat(dto.solvedCount()).isZero(); // no solvedCount calculation logic yet
    }

}
