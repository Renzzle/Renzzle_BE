package com.renzzle.backend.domain.puzzle.community.service;

import com.renzzle.backend.domain.puzzle.community.api.request.AddCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.api.response.AddCommunityPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzleAnswerResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzlesResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetSingleCommunityPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.projection.LikeDislikeProjection;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.community.domain.UserCommunityPuzzle;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.support.TestCommunityPuzzleBuilder;
import com.renzzle.backend.support.TestUserCommunityPuzzleBuilder;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.renzzle.backend.global.common.constant.ItemPrice.HINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommunityServiceTest {

    @Mock
    private Clock clock;
    @Mock
    private CommunityPuzzleRepository communityPuzzleRepository;
    @Mock
    private UserCommunityPuzzleRepository userCommunityPuzzleRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommunityService communityService;

    @Test
    public void addCommunityPuzzle_WhenValidInput_ThenSavesAndReturnsPuzzleId() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().build();

        AddCommunityPuzzleRequest request = new AddCommunityPuzzleRequest(
                "f8f9",
                "e5",
                7,
                "description",
                "BLACK"
        );

        CommunityPuzzle mockPuzzle = CommunityPuzzle.builder()
                .id(1L)
                .boardStatus(request.boardStatus())
                .boardKey(BoardUtils.makeBoardKey(request.boardStatus()))
                .answer(request.answer())
                .depth(request.depth())
                .rating(request.depth() * 200.0)
                .description(request.description())
                .user(user)
                .winColor(WinColor.getWinColor(request.winColor()))
                .build();

        when(communityPuzzleRepository.save(any(CommunityPuzzle.class)))
                .thenReturn(mockPuzzle);

        // When
        AddCommunityPuzzleResponse response = communityService.addCommunityPuzzle(request, user);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.puzzleId()).isEqualTo(mockPuzzle.getId());

        verify(communityPuzzleRepository, times(1)).save(any(CommunityPuzzle.class));
    }

    @Test
    void getCommunityPuzzleList_WhenCalled_ThenReturnsMappedResponse() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);

        when(communityPuzzleRepository.searchCommunityPuzzles(any(GetCommunityPuzzleRequest.class), anyLong()))
                .thenReturn(List.of(puzzle));
        when(userCommunityPuzzleRepository.checkIsSolvedPuzzle(anyLong(), anyLong()))
                .thenReturn(true);

        // When
        List<GetCommunityPuzzlesResponse> result =
                communityService.getCommunityPuzzleList(mock(GetCommunityPuzzleRequest.class), user);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(puzzle.getId());
    }

    @Test
    void getCommunityPuzzleById_WhenCalled_ThenReturnsExpectedResponse() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);

        LikeDislikeProjection projection = new LikeDislikeProjection() {
            public Boolean getIsLiked() { return true; }
            public Boolean getIsDisliked() { return false; }
        };

        when(communityPuzzleRepository.findById(puzzle.getId())).thenReturn(Optional.of(puzzle));
        when(userCommunityPuzzleRepository.checkIsSolvedPuzzle(user.getId(), puzzle.getId())).thenReturn(true);
        when(userCommunityPuzzleRepository.getMyLikeDislike(user.getId(), puzzle.getId())).thenReturn(Optional.of(projection));

        // When
        GetSingleCommunityPuzzleResponse result = communityService.getCommunityPuzzleById(puzzle.getId(), user);

        // Then
        assertThat(result.id()).isEqualTo(puzzle.getId());
        assertThat(result.isSolved()).isTrue();
        assertThat(result.myLike()).isTrue();
        assertThat(result.myDislike()).isFalse();
    }

    @Test
    void getCommunityPuzzleAnswer_WhenCalled_ThenReturnsAnswerAndCurrency() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().withCurrency(1000).save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user).withAnswer("e5").save(communityPuzzleRepository);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(communityPuzzleRepository.findById(puzzle.getId())).thenReturn(Optional.of(puzzle));

        // When
        GetCommunityPuzzleAnswerResponse result = communityService.getCommunityPuzzleAnswer(puzzle.getId(), user);

        // Then
        assertThat(result.answer()).isEqualTo("e5");
        assertThat(result.currency()).isEqualTo(user.getCurrency());
    }

    @Test
    void getCommunityPuzzleAnswer_WhenNotEnoughCurrency_ThenThrowsInsufficientCurrencyException() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().withCurrency(HINT.getPrice() - 1).save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user).withAnswer("e5").save(communityPuzzleRepository);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(communityPuzzleRepository.findById(puzzle.getId())).thenReturn(Optional.of(puzzle));

        // When
        CustomException exception = assertThrows(CustomException.class, () ->
                communityService.getCommunityPuzzleAnswer(puzzle.getId(), user)
        );

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_CURRENCY);
    }

    @Test
    void solveCommunityPuzzle_WhenNotSolved_ThenSavesNewRecord() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);

        when(communityPuzzleRepository.findById(puzzle.getId())).thenReturn(Optional.of(puzzle));
        when(userCommunityPuzzleRepository.solvePuzzle(user.getId(), puzzle.getId(), clock.instant()))
                .thenReturn(0);

        // When
        communityService.solveCommunityPuzzle(puzzle.getId(), user);

        // Then
        verify(userCommunityPuzzleRepository).save(any(UserCommunityPuzzle.class));
    }

    @Test
    void toggleLike_WhenUserPuzzleExists_ThenTogglesLike() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        UserCommunityPuzzle ucp = TestUserCommunityPuzzleBuilder.builder(user, puzzle)
                .withDisliked(true)
                .save(userCommunityPuzzleRepository);

        when(communityPuzzleRepository.findById(puzzle.getId())).thenReturn(Optional.of(puzzle));
        when(userCommunityPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), puzzle.getId()))
                .thenReturn(Optional.of(ucp));
        when(clock.instant()).thenReturn(Instant.MAX);

        // When
        boolean result = communityService.toggleLike(puzzle.getId(), user);

        // Then
        assertThat(result).isTrue();
        assertThat(ucp.isLiked()).isTrue();
        assertThat(ucp.isDisliked()).isFalse();
        assertThat(ucp.getLikedAt()).isEqualTo(Instant.MAX);
    }

    @Test
    void toggleLike_WhenUserPuzzleNotExists_ThenSaveNewRecord() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);

        when(communityPuzzleRepository.findById(puzzle.getId())).thenReturn(Optional.of(puzzle));
        when(userCommunityPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), puzzle.getId()))
                .thenReturn(Optional.empty());

        // When
        communityService.toggleLike(puzzle.getId(), user);

        // Then
        verify(userCommunityPuzzleRepository).save(any(UserCommunityPuzzle.class));
    }

    @Test
    void toggleDislike_WhenUserPuzzleExists_ThenTogglesDislike() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        UserCommunityPuzzle ucp = TestUserCommunityPuzzleBuilder.builder(user, puzzle)
                .withLiked(true)
                .save(userCommunityPuzzleRepository);

        when(communityPuzzleRepository.findById(puzzle.getId())).thenReturn(Optional.of(puzzle));
        when(userCommunityPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), puzzle.getId()))
                .thenReturn(Optional.of(ucp));

        // When
        boolean result = communityService.toggleDislike(puzzle.getId(), user);

        // Then
        assertThat(result).isTrue();
        assertThat(ucp.isLiked()).isFalse();
        assertThat(ucp.isDisliked()).isTrue();
    }

    @Test
    void toggleDislike_WhenUserPuzzleNotExists_ThenSaveNewRecord() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);

        when(communityPuzzleRepository.findById(puzzle.getId())).thenReturn(Optional.of(puzzle));
        when(userCommunityPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), puzzle.getId()))
                .thenReturn(Optional.empty());

        // When
        communityService.toggleDislike(puzzle.getId(), user);

        // Then
        verify(userCommunityPuzzleRepository).save(any(UserCommunityPuzzle.class));
    }

}
