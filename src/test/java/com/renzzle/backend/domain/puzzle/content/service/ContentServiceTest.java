package com.renzzle.backend.domain.puzzle.content.service;

import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzlesResponse;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.content.api.request.GetRecommendRequest;
import com.renzzle.backend.domain.puzzle.content.api.response.GetTrendPuzzlesResponse;
import com.renzzle.backend.domain.puzzle.content.api.response.GetRecommendPackResponse;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.training.dao.PackRepository;
import com.renzzle.backend.domain.puzzle.training.dao.PackTranslationRepository;
import com.renzzle.backend.domain.puzzle.training.dao.SolvedTrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.dao.UserPackRepository;
import com.renzzle.backend.domain.puzzle.training.dao.projection.PackLastSolvedProjection;
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

    private static final Difficulty LOW = Difficulty.getDifficulty("LOW");
    private static final Difficulty MIDDLE = Difficulty.getDifficulty("MIDDLE");
    private static final Difficulty HIGH = Difficulty.getDifficulty("HIGH");

    private UserEntity user;

    @BeforeEach
     void setup() {
        user = TestUserEntityBuilder.builder()
                .withId(1L)
                .withStatus(Status.getDefaultStatus())
                .build();
    }

    @Test
    void getRecommendedPack_WhenLatestPackIsUnfinished_ThenReturnsIt() {
        // Given: another pack is further along, but the latest solve was in this one
        Pack latest = pack(1L, LOW, 10);
        Pack furtherAlong = pack(2L, LOW, 10);
        givenPacks(List.of(latest, furtherAlong), List.of(progress(latest, 5), progress(furtherAlong, 9)), latest);

        // When
        GetRecommendPackResponse response = recommend();

        // Then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("title 1");
        assertThat(response.solvedPuzzleCount()).isEqualTo(5);
        assertThat(response.locked()).isFalse();
    }

    @Test
    void getRecommendedPack_WhenNothingSolved_ThenReturnsStarterPack() {
        // Given
        Pack starter = pack(Pack.STARTER_PACK_ID, LOW, 10);
        Pack other = pack(2L, LOW, 10);
        givenPacks(List.of(other, starter), List.of(progress(starter, 0)), null);

        // When
        GetRecommendPackResponse response = recommend();

        // Then
        assertThat(response.id()).isEqualTo(Pack.STARTER_PACK_ID);
        assertThat(response.solvedPuzzleCount()).isZero();
        assertThat(response.locked()).isFalse();
    }

    @Test
    void getRecommendedPack_WhenNoPackExists_ThenThrowsNoSuchTrainingPackException() {
        // Given
        when(packRepository.findByIsPublicTrue()).thenReturn(List.of());

        // When & Then
        assertThatThrownBy(this::recommend)
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.NO_SUCH_TRAINING_PACK.getMessage());
    }

    @Test
    void getRecommendedPack_WhenNoPackTranslationExists_ThenThrowsNoSuchPackTranslationException() {
        // Given
        Pack starter = pack(Pack.STARTER_PACK_ID, LOW, 10);
        when(packRepository.findByIsPublicTrue()).thenReturn(List.of(starter));
        when(packTranslationRepository.findByPackAndLangCode(eq(starter), any())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(this::recommend)
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.NO_SUCH_PACK_TRANSLATION.getMessage());
    }

    @Test
    void getRecommendedPack_WhenLatestPackIsFinished_ThenReturnsTheFurthestAlongPartwayPack() {
        // Given: 8/10 is further along than 15/40, though 15/40 was played more recently
        Pack finished = pack(1L, LOW, 10);
        Pack moreSolved = pack(2L, LOW, 40);
        Pack furtherAlong = pack(3L, LOW, 10);
        givenPacks(List.of(finished, moreSolved, furtherAlong),
                List.of(progress(finished, 10), progress(moreSolved, 15), progress(furtherAlong, 8)), finished);
        when(solvedTrainingPuzzleRepository.findLastSolvedAtByPack(user.getId())).thenReturn(List.of(
                lastSolved(2L, Instant.parse("2025-01-02T00:00:00Z")),
                lastSolved(3L, Instant.parse("2025-01-01T00:00:00Z"))));

        // When
        GetRecommendPackResponse response = recommend();

        // Then
        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.solvedPuzzleCount()).isEqualTo(8);
    }

    @Test
    void getRecommendedPack_WhenPartwayPacksAreEquallyFarAlong_ThenReturnsTheOnePlayedLast() {
        // Given: both halfway
        Pack finished = pack(1L, LOW, 10);
        Pack playedEarlier = pack(2L, LOW, 10);
        Pack playedLater = pack(3L, LOW, 20);
        givenPacks(List.of(finished, playedEarlier, playedLater),
                List.of(progress(finished, 10), progress(playedEarlier, 5), progress(playedLater, 10)), finished);
        when(solvedTrainingPuzzleRepository.findLastSolvedAtByPack(user.getId())).thenReturn(List.of(
                lastSolved(2L, Instant.parse("2025-01-01T00:00:00Z")),
                lastSolved(3L, Instant.parse("2025-01-02T00:00:00Z"))));

        // When
        GetRecommendPackResponse response = recommend();

        // Then
        assertThat(response.id()).isEqualTo(3L);
    }

    @Test
    void getRecommendedPack_WhenNothingIsPartway_ThenReturnsAnOwnedPackOfTheSameDifficultyFirst() {
        // Given: an unowned pack and an empty one come before the owned pack in id order
        Pack finished = pack(1L, MIDDLE, 10);
        Pack unowned = pack(2L, MIDDLE, 10);
        Pack empty = pack(3L, MIDDLE, 0);
        Pack ownedPack = pack(4L, MIDDLE, 10);
        Pack easier = pack(5L, LOW, 10);
        givenPacks(List.of(finished, unowned, empty, ownedPack, easier),
                List.of(progress(finished, 10), progress(empty, 0), progress(ownedPack, 0), progress(easier, 0)),
                finished);

        // When
        GetRecommendPackResponse response = recommend();

        // Then
        assertThat(response.id()).isEqualTo(4L);
        assertThat(response.locked()).isFalse();
    }

    @Test
    void getRecommendedPack_WhenSeveralOwnedPacksAreUnstarted_ThenReturnsTheFirstInThePackList() {
        // Given: creator 10 started first, so pack 4 lists before pack 3
        Pack finished = pack(1L, LOW, 10);
        Pack creatorAFinished = pack(2L, LOW, 10).toBuilder().createdBy(10L).build();
        Pack creatorB = pack(3L, LOW, 10).toBuilder().createdBy(20L).build();
        Pack creatorA = pack(4L, LOW, 10).toBuilder().createdBy(10L).build();
        givenPacks(List.of(finished, creatorAFinished, creatorB, creatorA),
                List.of(progress(finished, 10), progress(creatorAFinished, 10), progress(creatorB, 0), progress(creatorA, 0)),
                finished);

        // When
        GetRecommendPackResponse response = recommend();

        // Then
        assertThat(response.id()).isEqualTo(4L);
    }

    @Test
    void getRecommendedPack_WhenSameDifficultyIsDone_ThenReturnsTheNextHarderPackLocked() {
        // Given: an owned easier pack is left, but a harder one comes first
        Pack finished = pack(1L, MIDDLE, 10);
        Pack alsoFinished = pack(2L, MIDDLE, 10);
        Pack easier = pack(3L, LOW, 10);
        Pack harder = TestPackBuilder.builder().withId(4L).withDifficulty(HIGH).withPuzzleCount(10).withPrice(3000).build();
        givenPacks(List.of(finished, alsoFinished, easier, harder),
                List.of(progress(finished, 10), progress(alsoFinished, 10), progress(easier, 0)), finished);

        // When
        GetRecommendPackResponse response = recommend();

        // Then
        assertThat(response.id()).isEqualTo(4L);
        assertThat(response.locked()).isTrue();
        assertThat(response.price()).isEqualTo(3000);
        assertThat(response.solvedPuzzleCount()).isZero();
    }

    @Test
    void getRecommendedPack_WhenNothingHarderIsLeft_ThenReturnsTheNearestEasierDifficulty() {
        // Given
        Pack finished = pack(1L, HIGH, 10);
        Pack lowest = pack(2L, LOW, 10);
        Pack middle = pack(3L, MIDDLE, 10);
        givenPacks(List.of(finished, lowest, middle),
                List.of(progress(finished, 10), progress(lowest, 0), progress(middle, 0)), finished);

        // When
        GetRecommendPackResponse response = recommend();

        // Then
        assertThat(response.id()).isEqualTo(3L);
    }

    @Test
    void getRecommendedPack_WhenEveryPackIsFinished_ThenKeepsTheLatestOne() {
        // Given
        Pack earlier = pack(1L, LOW, 5);
        Pack latest = pack(2L, LOW, 10);
        givenPacks(List.of(earlier, latest), List.of(progress(earlier, 5), progress(latest, 10)), latest);

        // When
        GetRecommendPackResponse response = recommend();

        // Then
        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.solvedPuzzleCount()).isEqualTo(10);
        assertThat(response.totalPuzzleCount()).isEqualTo(10);
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

    private GetRecommendPackResponse recommend() {
        return contentService.getRecommendedPack(new GetRecommendRequest("EN"), user);
    }

    // The public packs, the ones the user has, and the pack of their latest solve (null when nothing is solved)
    private void givenPacks(List<Pack> packs, List<UserPack> owned, Pack latest) {
        when(packRepository.findByIsPublicTrue()).thenReturn(packs);
        when(userPackRepository.findAllByUserIdAndPackIdIn(eq(user.getId()), anyList())).thenReturn(owned);
        when(solvedTrainingPuzzleRepository.findLatestInPublicPackByUserId(user.getId()))
                .thenReturn(Optional.ofNullable(latest).map(this::solveIn));
        when(packTranslationRepository.findByPackAndLangCode(any(), any()))
                .thenAnswer(invocation -> Optional.of(translationOf(invocation.getArgument(0))));
    }

    private static Pack pack(Long id, Difficulty difficulty, int puzzleCount) {
        return TestPackBuilder.builder().withId(id).withDifficulty(difficulty).withPuzzleCount(puzzleCount).build();
    }

    private UserPack progress(Pack pack, int solved) {
        return UserPack.builder().user(user).pack(pack).solvedCount(solved).build();
    }

    private SolvedTrainingPuzzle solveIn(Pack pack) {
        return SolvedTrainingPuzzle.builder().user(user).puzzle(TrainingPuzzle.builder().pack(pack).build()).build();
    }

    private static PackTranslation translationOf(Pack pack) {
        return PackTranslation.builder()
                .pack(pack)
                .langCode(LangCode.getLangCode("EN"))
                .title("title " + pack.getId())
                .author("author")
                .description("description")
                .build();
    }

    private static PackLastSolvedProjection lastSolved(Long packId, Instant lastSolvedAt) {
        return new PackLastSolvedProjection() {
            @Override
            public Long getPackId() {
                return packId;
            }

            @Override
            public Instant getLastSolvedAt() {
                return lastSolvedAt;
            }
        };
    }
}
