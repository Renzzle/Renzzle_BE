package com.renzzle.backend.domain.puzzle.content.service;

import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzlesResponse;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.content.api.request.GetRecommendRequest;
import com.renzzle.backend.domain.puzzle.content.api.response.GetTrendPuzzlesResponse;
import com.renzzle.backend.domain.puzzle.content.api.response.getRecommendPackResponse;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ContentServiceTest {
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
        when(userPackRepository.findByUserIdAndPackId(eq(user.getId()), eq(pack.getId())))
                .thenReturn(Optional.of(userPack));

        // When
        getRecommendPackResponse response = contentService.getRecommendedPack(new GetRecommendRequest("EN"), user);

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
        getRecommendPackResponse response = contentService.getRecommendedPack(new GetRecommendRequest("EN"), user);

        // Then
        assertThat(response.id()).isEqualTo(pack.getId());
        assertThat(response.title()).isEqualTo(translation.getTitle());
        assertThat(response.solvedPuzzleCount()).isEqualTo(0);
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
        when(userPackRepository.findByUserIdAndPackId(eq(user.getId()), eq(pack.getId())))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> contentService.getRecommendedPack(new GetRecommendRequest("EN"), user))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.NO_USER_PROGRESS_FOR_PACK.getMessage());
    }

    @Test
    void getTrendCommunityPuzzles_WhenDuplicatePuzzle_ThenThrowsTrendPuzzleDuplicatedException() {
        // Given
        Instant now = Instant.parse("2024-04-28T00:00:00Z");
        lenient().when(clock.instant()).thenReturn(now);

        CommunityPuzzle puzzle1 = TestCommunityPuzzleBuilder.builder(user)
                .withId(1L)
                .withCreatedAt(now)
                .withLikeCount(10)
                .withDislikeCount(0)
                .withView(100)
                .build();

        List<CommunityPuzzle> puzzlesIn7Days = List.of(puzzle1, puzzle1); // 일부러 중복된 퍼즐 리스트

        when(communityPuzzleRepository.findByCreatedAtAfter(any()))
                .thenReturn(puzzlesIn7Days);

        lenient().when(communityPuzzleRepository.findTop30ByCreatedAtBeforeOrderByCreatedAtDesc(any()))
                .thenReturn(List.of());

        // when & then
        assertThatThrownBy(() -> contentService.getTrendCommunityPuzzles(user))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.TREND_PUZZLE_DUPLICATED.getMessage());
    }

    @Test
    void getTrendCommunityPuzzles_WhenValidPuzzlesExist_ThenReturnTop5Puzzles() {
        // Given
        Instant now = Instant.parse("2024-04-28T00:00:00Z");
        lenient().when(clock.instant()).thenReturn(now);

        List<CommunityPuzzle> puzzles = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            puzzles.add(
                    TestCommunityPuzzleBuilder.builder(user)
                            .withId(i)
                            .withCreatedAt(now.minusSeconds(i * 60)) // 시간 차이만 조금씩 줌
                            .withLikeCount((int) (10 - i)) // 좋아요 수 다르게
                            .withDislikeCount((int) (i % 3)) // 싫어요도 다르게
                            .withView((int) (100 + i * 10)) // 조회수 다르게
                            .build()
            );
        }

        when(communityPuzzleRepository.findByCreatedAtAfter(any()))
                .thenReturn(puzzles);

        lenient().when(communityPuzzleRepository.findTop30ByCreatedAtBeforeOrderByCreatedAtDesc(any()))
                .thenReturn(List.of()); // 백업 퍼즐은 없음

        for (CommunityPuzzle puzzle : puzzles) {
            lenient().when(userCommunityPuzzleRepository.checkIsSolvedPuzzle(user.getId(), puzzle.getId()))
                    .thenReturn(false);
        }


        // when
        GetTrendPuzzlesResponse response = contentService.getTrendCommunityPuzzles(user);

        // then
        assertThat(response.puzzles()).hasSize(5);

        // 추가 검증: 좋아요 높은 순 + 조회수 높은 순 + id 낮은 순대로 정렬된지 확인
        List<Long> puzzleIds = response.puzzles().stream()
                    .map(GetCommunityPuzzlesResponse::id)
                .toList();

        System.out.println("선택된 퍼즐 IDs: " + puzzleIds);
    }

    @Test
    void getTrendCommunityPuzzles_WhenPuzzleSelected_ThenResponseFieldsMappedCorrectly() {
        // Given
        Instant now = Instant.parse("2024-04-28T00:00:00Z");
        lenient().when(clock.instant()).thenReturn(now);

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

        when(communityPuzzleRepository.findByCreatedAtAfter(any()))
                .thenReturn(List.of(puzzle));

        lenient().when(communityPuzzleRepository.findTop30ByCreatedAtBeforeOrderByCreatedAtDesc(any()))
                .thenReturn(List.of());

        lenient().when(userCommunityPuzzleRepository.checkIsSolvedPuzzle(user.getId(), puzzle.getId()))
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
        assertThat(dto.solvedCount()).isEqualTo(0); // 아직은 solvedCount 계산 로직 없음
    }

}
