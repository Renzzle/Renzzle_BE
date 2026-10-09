package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.domain.appinfo.service.AppInfoService;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.projection.AuthorStatsProjection;
import com.renzzle.backend.domain.puzzle.community.dao.projection.SolvedCountProjection;
import com.renzzle.backend.domain.puzzle.rank.api.request.RankResultRequest;
import com.renzzle.backend.domain.puzzle.rank.api.response.*;
import com.renzzle.backend.domain.puzzle.rank.dao.LatestRankPuzzleRepository;
import com.renzzle.backend.domain.puzzle.rank.domain.LatestRankPuzzle;
import com.renzzle.backend.domain.puzzle.rank.domain.RankSessionData;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.shared.util.ELOUtils;
import com.renzzle.backend.domain.puzzle.shared.util.RatingUtil;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.dao.projection.UserNicknameProjection;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.constant.ItemPrice;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.renzzle.backend.global.common.constant.ItemPrice.RANK_REWARD;
import static com.renzzle.backend.global.common.constant.StringConstant.DELETED_USER;
import static com.renzzle.backend.support.TestTime.FIXED_INSTANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankServiceTest {
    private RankService rankService;
    @Mock
    private StringRedisTemplate redisRankingTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOperations;
    @Mock
    private RedisTemplate<String, RankSessionData> redisSessionTemplate;
    @Mock
    private ValueOperations<String, RankSessionData> valueOperations;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TrainingPuzzleRepository trainingPuzzleRepository;
    @Mock
    private CommunityPuzzleRepository communityPuzzleRepository;
    @Mock
    private LatestRankPuzzleRepository latestRankPuzzleRepository;
    @Mock
    private UserCommunityPuzzleRepository userCommunityPuzzleRepository;
    @Mock
    private Clock clock;
    @Mock
    private AppInfoService appInfoService;
    @BeforeEach
    void setup() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2025-01-01T00:00:00Z"));
        lenient().when(appInfoService.getPrice(any())).thenAnswer(invocation -> invocation.<ItemPrice>getArgument(0).getDefaultPrice());
        rankService = new RankService(
                redisSessionTemplate,
                trainingPuzzleRepository,
                communityPuzzleRepository,
                userRepository,
                latestRankPuzzleRepository,
                userCommunityPuzzleRepository,
                clock,
                redisRankingTemplate,
                appInfoService
        );

        lenient().when(redisSessionTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // startRankGame Test
    @Test
    void startRankGame_WhenUserNotFound_ThenThrowsCustomException() {
        // Given
        UserEntity dummy = TestUserFactory.createTestUser("noname", 1500.0);
        when(userRepository.findByIdForUpdate(dummy.getId())).thenReturn(Optional.empty());

        // When
        CustomException ex = assertThrows(CustomException.class, () ->
                rankService.startRankGame(dummy)
        );
        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CANNOT_FIND_USER);
    }

    @Test
    void startRankGame_WhenValidUser_ThenReturnsResponseAndStoresSession() {
        // Given
        UserEntity user = TestUserFactory.createTestUser("tester", 1500.0);
        ReflectionTestUtils.setField(user, "id", 1L); // force-inject the ID

        TrainingPuzzle puzzle = TrainingPuzzle.builder()
                .boardStatus("a1a2")
                .answer("a3")
                .rating(1400)
                .winColor(WinColor.getWinColor("BLACK"))
                .depth(3)
                .build();

        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(latestRankPuzzleRepository.findAllByUser(user)).thenReturn(Collections.emptyList());

        when(trainingPuzzleRepository.findAvailableTrainingPuzzlesSortedByRating(user))
                .thenReturn(List.of(puzzle));
        when(communityPuzzleRepository.findAvailableCommunityPuzzlesSortedByRating(user))
                .thenReturn(Collections.emptyList());

        when(redisSessionTemplate.opsForValue()).thenReturn(valueOperations);
        when(clock.instant()).thenReturn(Instant.parse("2025-01-01T00:00:00Z"));
        // When
        RankStartResponse response = rankService.startRankGame(user);
        // Then
        assertThat(response.boardStatus()).isEqualTo("a1a2");
        assertThat(response.winColor()).isEqualTo("BLACK");

        verify(valueOperations).set(eq("1"), any(RankSessionData.class), anyLong(), eq(TimeUnit.SECONDS));

        verify(latestRankPuzzleRepository).save(any());
        verify(userRepository).save(argThat(savedUser ->
                savedUser.getRating() < 1500.0 &&
                        savedUser.getMmr() < 1500.0
        ));
    }

    @Test
    void startRankGame_WhenRatingAndMmrDiffer_ThenDerivesEachPenaltyFromItsOwnValue() {
        // Given: rating and mmr differ, so swapped penalty arguments would show
        UserEntity user = TestUserFactory.createTestUser("tester", 1400.0);
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "mmr", 1600.0);

        TrainingPuzzle puzzle = TrainingPuzzle.builder()
                .boardStatus("a1a2")
                .answer("a3")
                .rating(1400)
                .winColor(WinColor.getWinColor("BLACK"))
                .depth(3)
                .build();

        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(latestRankPuzzleRepository.findAllByUser(user)).thenReturn(Collections.emptyList());
        when(trainingPuzzleRepository.findAvailableTrainingPuzzlesSortedByRating(user))
                .thenReturn(List.of(puzzle));
        when(communityPuzzleRepository.findAvailableCommunityPuzzlesSortedByRating(user))
                .thenReturn(Collections.emptyList());
        when(redisSessionTemplate.opsForValue()).thenReturn(valueOperations);
        when(clock.instant()).thenReturn(Instant.parse("2025-01-01T00:00:00Z"));

        // When
        rankService.startRankGame(user);

        // mmr 1600 (x1.5) -> 1577, rating 1400 (x0.5) -> 1398; swapped would give 1595 / 1389
        ArgumentCaptor<UserEntity> saved = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(saved.capture());

        assertThat(saved.getValue().getMmr()).isEqualTo(1577.0);
        assertThat(saved.getValue().getRating()).isEqualTo(1398.0);
    }

    @Test
    void resultRankGame_WhenSessionIsNull_ThenThrowsEmptySession() {
        //Given
        UserEntity user = TestUserFactory.createTestUser("u1", 1500);
        ReflectionTestUtils.setField(user, "id", 1L);

        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.get("1")).thenReturn(null);

        RankResultRequest request = new RankResultRequest(true, null);

        // When
        CustomException ex = assertThrows(CustomException.class, () -> rankService.resultRankGame(user, request));
        //Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMPTY_SESSION_DATA);
    }

    @Test
    void resultRankGame_WhenSessionNotStarted_ThenThrowsEmptySession() {
        //Given
        UserEntity user = TestUserFactory.createTestUser("u1", 1500);
        ReflectionTestUtils.setField(user, "id", 1L);

        RankSessionData session = new RankSessionData();
        session.setStarted(false);

        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.get("1")).thenReturn(session);

        RankResultRequest request = new RankResultRequest(true, null);

        // When
        CustomException ex = assertThrows(CustomException.class, () -> rankService.resultRankGame(user, request));
        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMPTY_SESSION_DATA);
    }

    @Test
    void resultRankGame_WhenSessionTtlInvalid_ThenThrowsInvalidSessionTTL() {
        //Given
        UserEntity user = TestUserFactory.createTestUser("u1", 1500);
        ReflectionTestUtils.setField(user, "id", 1L);

        RankSessionData session = new RankSessionData();
        session.setStarted(true);

        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.get("1")).thenReturn(session);
        when(redisSessionTemplate.getExpire("1", TimeUnit.SECONDS)).thenReturn(0L); // or null

        RankResultRequest request = new RankResultRequest(true, null);
        // When
        CustomException ex = assertThrows(CustomException.class, () -> rankService.resultRankGame(user, request));
        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_SESSION_TTL);
    }

    @Test
    void resultRankGame_WhenPreviousPuzzleNotFound_ThenThrowsPuzzleNotFound() {
        //Given
        UserEntity user = TestUserFactory.createTestUser("u1", 1500);
        ReflectionTestUtils.setField(user, "id", 1L);

        RankSessionData session = new RankSessionData();
        session.setStarted(true);

        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.get("1")).thenReturn(session);
        when(redisSessionTemplate.getExpire("1", TimeUnit.SECONDS)).thenReturn(60L);
        when(latestRankPuzzleRepository.findTopByUserOrderByIdDesc(user)).thenReturn(Optional.empty());
        // When
        RankResultRequest request = new RankResultRequest(true, null);

        CustomException ex = assertThrows(CustomException.class, () -> rankService.resultRankGame(user, request));
        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LATEST_PUZZLE_NOT_FOUND);
    }

    @Test
    void resultRankGame_WhenSolvedCorrectly_ThenReturnsNextPuzzle() {
        // Given
        UserEntity user = TestUserFactory.createTestUser("u1", 1500);
        ReflectionTestUtils.setField(user, "id", 1L);

        RankSessionData session = new RankSessionData();
        session.setStarted(true);

        // Existing problem (previous round's problem)
        LatestRankPuzzle previous = LatestRankPuzzle.builder()
                .user(user)
                .boardStatus("a1a2")
                .answer("a3")
                .isSolved(false)
                .assignedAt(clock.instant())
                .winColor(WinColor.getWinColor("WHITE"))
                .puzzleRating(1400)
                .ratingBeforePenalty(1500)
                .mmrBeforePenalty(1500)
                .targetWinProbability(0.7)
                .build();

        // Next problem candidate (TrainingPuzzle)
        TrainingPuzzle candidatePuzzle = TrainingPuzzle.builder()
                .boardStatus("nextBoard")
                .answer("nextAnswer")
                .depth(3)
                .rating(1450)
                .winColor(WinColor.getWinColor("BLACK"))
                .build();


        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.get("1")).thenReturn(session);
        when(redisSessionTemplate.getExpire("1", TimeUnit.SECONDS)).thenReturn(120L);
        when(latestRankPuzzleRepository.findTopByUserOrderByIdDesc(user)).thenReturn(Optional.of(previous));
        when(trainingPuzzleRepository.findAvailableTrainingPuzzlesSortedByRating(user)).thenReturn(List.of(candidatePuzzle));
        when(communityPuzzleRepository.findAvailableCommunityPuzzlesSortedByRating(user)).thenReturn(Collections.emptyList());
        when(clock.instant()).thenReturn(Instant.parse("2025-01-01T00:00:00Z"));

        RankResultRequest request = new RankResultRequest(true, null);
        RankResultResponse response = rankService.resultRankGame(user, request);

        // Then
        assertThat(response.boardStatus()).isEqualTo("nextBoard");
        assertThat(response.winColor()).isEqualTo("BLACK");

        verify(redisSessionTemplate.opsForValue())
                .setIfPresent(eq("1"), any(RankSessionData.class), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void resultRankGame_WhenSolvedAtMinTargetWinProbability_ThenNextAssignmentStaysAtMin() {
        // Given: already at the floor
        UserEntity user = TestUserFactory.createTestUser("u1", 1500);
        ReflectionTestUtils.setField(user, "id", 1L);

        RankSessionData session = new RankSessionData();
        session.setStarted(true);

        LatestRankPuzzle previous = LatestRankPuzzle.builder()
                .user(user)
                .boardStatus("a1a2")
                .answer("a3")
                .isSolved(false)
                .assignedAt(clock.instant())
                .winColor(WinColor.getWinColor("BLACK"))
                .puzzleRating(2000)
                .ratingBeforePenalty(1500)
                .mmrBeforePenalty(1500)
                .targetWinProbability(ELOUtils.MIN_TARGET_WIN_PROBABILITY)
                .build();

        TrainingPuzzle candidatePuzzle = TrainingPuzzle.builder()
                .boardStatus("nextBoard")
                .answer("nextAnswer")
                .depth(3)
                .rating(2000)
                .winColor(WinColor.getWinColor("BLACK"))
                .build();

        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.get("1")).thenReturn(session);
        when(redisSessionTemplate.getExpire("1", TimeUnit.SECONDS)).thenReturn(120L);
        when(latestRankPuzzleRepository.findTopByUserOrderByIdDesc(user)).thenReturn(Optional.of(previous));
        when(trainingPuzzleRepository.findAvailableTrainingPuzzlesSortedByRating(user)).thenReturn(List.of(candidatePuzzle));
        when(communityPuzzleRepository.findAvailableCommunityPuzzlesSortedByRating(user)).thenReturn(Collections.emptyList());

        // When
        rankService.resultRankGame(user, new RankResultRequest(true, null));

        // Then
        ArgumentCaptor<LatestRankPuzzle> assigned = ArgumentCaptor.forClass(LatestRankPuzzle.class);
        verify(latestRankPuzzleRepository).save(assigned.capture());
        assertThat(assigned.getValue().getTargetWinProbability())
                .isEqualTo(ELOUtils.MIN_TARGET_WIN_PROBABILITY);
    }

    @Test
    void startRankGame_WhenPuzzleAssigned_ThenRecordsWhichPuzzleItWas() {
        // Given
        UserEntity user = TestUserFactory.createTestUser("tester", 1500.0);
        ReflectionTestUtils.setField(user, "id", 1L);

        TrainingPuzzle puzzle = TrainingPuzzle.builder()
                .boardStatus("a1a2")
                .answer("a3")
                .rating(1400)
                .winColor(WinColor.getWinColor("BLACK"))
                .depth(3)
                .build();
        ReflectionTestUtils.setField(puzzle, "id", 7L);

        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(latestRankPuzzleRepository.findAllByUser(user)).thenReturn(Collections.emptyList());
        when(trainingPuzzleRepository.findAvailableTrainingPuzzlesSortedByRating(user)).thenReturn(List.of(puzzle));
        when(communityPuzzleRepository.findAvailableCommunityPuzzlesSortedByRating(user)).thenReturn(Collections.emptyList());

        // When
        rankService.startRankGame(user);

        // Then
        ArgumentCaptor<LatestRankPuzzle> assigned = ArgumentCaptor.forClass(LatestRankPuzzle.class);
        verify(latestRankPuzzleRepository).save(assigned.capture());
        assertThat(assigned.getValue().getPuzzleType()).isEqualTo(PuzzleType.TRAINING);
        assertThat(assigned.getValue().getPuzzleId()).isEqualTo(7L);
    }

    @Test
    void resultRankGame_WhenTrainingPuzzleSolved_ThenLowersThatPuzzleRating() {
        // Given
        UserEntity user = givenAnsweredRound(PuzzleType.TRAINING, 7L);
        when(trainingPuzzleRepository.findRankAttemptCountById(7L)).thenReturn(Optional.of(0));

        // When
        rankService.resultRankGame(user, new RankResultRequest(true, null));

        // Then: evenly matched and never attempted, so K = 40 and expected = 0.5
        verify(trainingPuzzleRepository).applyRankResult(7L, -20.0, RatingUtil.MIN_RATING, RatingUtil.MAX_RATING);
        verify(communityPuzzleRepository, never()).applyRankResult(anyLong(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void resultRankGame_WhenCommunityPuzzleMissed_ThenRaisesThatPuzzleRating() {
        // Given
        UserEntity user = givenAnsweredRound(PuzzleType.COMMUNITY, 9L);
        when(communityPuzzleRepository.findRankAttemptCountById(9L)).thenReturn(Optional.of(0));

        // When
        rankService.resultRankGame(user, new RankResultRequest(false, null));

        // Then: evenly matched and never attempted, so community K = 60 and expected = 0.5
        verify(communityPuzzleRepository).applyRankResult(9L, 30.0, RatingUtil.MIN_RATING, RatingUtil.MAX_RATING);
        verify(trainingPuzzleRepository, never()).applyRankResult(anyLong(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void resultRankGame_WhenPuzzleAlreadyWellAttempted_ThenMovesItLess() {
        // Given
        UserEntity user = givenAnsweredRound(PuzzleType.TRAINING, 7L);
        when(trainingPuzzleRepository.findRankAttemptCountById(7L)).thenReturn(Optional.of(10));

        // When
        rankService.resultRankGame(user, new RankResultRequest(false, null));

        // Then: K is down to 20 after 10 results
        verify(trainingPuzzleRepository).applyRankResult(7L, 10.0, RatingUtil.MIN_RATING, RatingUtil.MAX_RATING);
    }

    @Test
    void resultRankGame_WhenAssignmentHasNoPuzzleReference_ThenLeavesPuzzleRatingsAlone() {
        // Given: a row assigned before the puzzle reference was recorded
        UserEntity user = givenAnsweredRound(null, null);

        // When
        rankService.resultRankGame(user, new RankResultRequest(true, null));

        // Then
        verify(trainingPuzzleRepository, never()).applyRankResult(anyLong(), anyDouble(), anyDouble(), anyDouble());
        verify(communityPuzzleRepository, never()).applyRankResult(anyLong(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void resultRankGame_WhenPuzzleDeletedSinceAssignment_ThenSkipsIt() {
        // Given
        UserEntity user = givenAnsweredRound(PuzzleType.COMMUNITY, 9L);
        when(communityPuzzleRepository.findRankAttemptCountById(9L)).thenReturn(Optional.empty());

        // When
        rankService.resultRankGame(user, new RankResultRequest(false, null));

        // Then
        verify(communityPuzzleRepository, never()).applyRankResult(anyLong(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void resultRankGame_WhenBoardIsTheCurrentPuzzle_ThenAppliesTheResult() {
        // Given
        UserEntity user = givenAnsweredRound(PuzzleType.TRAINING, 7L);

        // When
        RankResultResponse response = rankService.resultRankGame(user, new RankResultRequest(true, "a1a2"));

        // Then
        assertThat(response.boardStatus()).isEqualTo("nextBoard");
    }

    @Test
    void resultRankGame_WhenResentForThePuzzleJustAnswered_ThenReturnsTheCurrentPuzzleWithoutApplying() {
        // Given: the answer to a1a2 went through, but its response with c1c2 never reached the app
        UserEntity user = TestUserFactory.createTestUser("u1", 1000);
        ReflectionTestUtils.setField(user, "id", 1L);
        LatestRankPuzzle current = givenResendableRound(user);

        // When: the app, still showing a1a2, sends a result again
        RankResultResponse response = rankService.resultRankGame(user, new RankResultRequest(false, "a1a2"));

        // Then
        assertThat(response.boardStatus()).isEqualTo("c1c2");
        assertThat(response.winColor()).isEqualTo("WHITE");
        assertThat(current.getIsSolved()).isFalse();
        assertThat(user.getRating()).isEqualTo(1000.0);
        verify(latestRankPuzzleRepository, never()).save(any());
    }

    @Test
    void resultRankGame_WhenBoardMatchesNeitherRecentPuzzle_ThenRejectsWithoutApplying() {
        // Given
        UserEntity user = TestUserFactory.createTestUser("u1", 1000);
        ReflectionTestUtils.setField(user, "id", 1L);
        givenResendableRound(user);

        // When
        CustomException ex = assertThrows(CustomException.class,
                () -> rankService.resultRankGame(user, new RankResultRequest(true, "z9z9")));

        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RANK_PUZZLE_MISMATCH);
        verify(latestRankPuzzleRepository, never()).save(any());
    }

    // a1a2 already answered, c1c2 handed out next; returns the c1c2 assignment
    private LatestRankPuzzle givenResendableRound(UserEntity user) {
        RankSessionData session = new RankSessionData();
        session.setStarted(true);

        LatestRankPuzzle answered = LatestRankPuzzle.builder()
                .id(10L)
                .user(user)
                .boardStatus("a1a2")
                .isSolved(true)
                .winColor(WinColor.getWinColor("BLACK"))
                .assignedAt(clock.instant())
                .build();
        LatestRankPuzzle current = LatestRankPuzzle.builder()
                .id(11L)
                .user(user)
                .boardStatus("c1c2")
                .isSolved(false)
                .winColor(WinColor.getWinColor("WHITE"))
                .assignedAt(clock.instant())
                .build();

        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.get("1")).thenReturn(session);
        when(redisSessionTemplate.getExpire("1", TimeUnit.SECONDS)).thenReturn(120L);
        when(latestRankPuzzleRepository.findTopByUserOrderByIdDesc(user)).thenReturn(Optional.of(current));
        when(latestRankPuzzleRepository.findTopByUserAndIdLessThanOrderByIdDesc(user, 11L))
                .thenReturn(Optional.of(answered));
        return current;
    }

    // A started game whose current puzzle (mmr 1000 vs puzzle 1000) is about to be answered
    private UserEntity givenAnsweredRound(PuzzleType puzzleType, Long puzzleId) {
        UserEntity user = TestUserFactory.createTestUser("u1", 1000);
        ReflectionTestUtils.setField(user, "id", 1L);

        RankSessionData session = new RankSessionData();
        session.setStarted(true);

        LatestRankPuzzle answered = LatestRankPuzzle.builder()
                .user(user)
                .puzzleType(puzzleType)
                .puzzleId(puzzleId)
                .boardStatus("a1a2")
                .answer("a3")
                .isSolved(false)
                .assignedAt(clock.instant())
                .winColor(WinColor.getWinColor("BLACK"))
                .puzzleRating(1000)
                .ratingBeforePenalty(1000)
                .mmrBeforePenalty(1000)
                .targetWinProbability(0.7)
                .build();

        TrainingPuzzle nextCandidate = TrainingPuzzle.builder()
                .boardStatus("nextBoard")
                .answer("nextAnswer")
                .depth(3)
                .rating(1000)
                .winColor(WinColor.getWinColor("BLACK"))
                .build();

        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.get("1")).thenReturn(session);
        when(redisSessionTemplate.getExpire("1", TimeUnit.SECONDS)).thenReturn(120L);
        when(latestRankPuzzleRepository.findTopByUserOrderByIdDesc(user)).thenReturn(Optional.of(answered));
        when(trainingPuzzleRepository.findAvailableTrainingPuzzlesSortedByRating(user)).thenReturn(List.of(nextCandidate));
        when(communityPuzzleRepository.findAvailableCommunityPuzzlesSortedByRating(user)).thenReturn(Collections.emptyList());
        return user;
    }

    // endRankGame test

    @Test
    void endRankGame_WhenSessionIsNull_ThenThrowsException() {
        // Given
        UserEntity user = TestUserFactory.createTestUser("user1", 1500);
        ReflectionTestUtils.setField(user, "id", 1L);

        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.get("1")).thenReturn(null); // no session
        // When
        CustomException ex = assertThrows(CustomException.class, () ->
                rankService.endRankGame(user)
        );
        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMPTY_SESSION_DATA);
    }

    @Test
    void endRankGame_WhenSessionNotStarted_ThenThrowsException() {
        // Given
        UserEntity user = TestUserFactory.createTestUser("user2", 1500);
        ReflectionTestUtils.setField(user, "id", 2L);

        RankSessionData session = new RankSessionData();
        session.setStarted(false); // session that has not been started

        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.get("2")).thenReturn(session);
        // When
        CustomException ex = assertThrows(CustomException.class, () ->
                rankService.endRankGame(user)
        );
        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.IS_NOT_STARTED);
    }

    @Test
    void endRankGame_WhenValidSession_ThenDeletesSessionAndReturnsRating() {
        // Given
        UserEntity user = TestUserFactory.createTestUser("user3", 1600);
        ReflectionTestUtils.setField(user, "id", 3L);

        RankSessionData session = new RankSessionData();
        session.setStarted(true);

        when(valueOperations.get("3")).thenReturn(session);
        when(redisSessionTemplate.getExpire("3", TimeUnit.SECONDS)).thenReturn(60L);
        when(redisSessionTemplate.delete("3")).thenReturn(true);
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(latestRankPuzzleRepository.findAllByUser(user))
                .thenReturn(List.of(
                        LatestRankPuzzle.builder().isSolved(true).build(),
                        LatestRankPuzzle.builder().isSolved(true).build()
                ));
        // When
        RankEndResponse response = rankService.endRankGame(user);
        // Then
        assertThat(response.rating()).isEqualTo(1600);
        assertThat(response.reward()).isEqualTo(2 * RANK_REWARD.getDefaultPrice());
        verify(redisSessionTemplate).delete("3");
    }

    @Test
    void endRankGame_WhenSessionAlreadyClaimedByAnotherEnd_ThenThrowsWithoutReward() {
        // Given: a repeated end read the session, but the first end deleted it first
        UserEntity user = TestUserFactory.createTestUser("user3", 1600);
        ReflectionTestUtils.setField(user, "id", 3L);
        int currencyBefore = user.getCurrency();

        RankSessionData session = new RankSessionData();
        session.setStarted(true);

        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.get("3")).thenReturn(session);
        when(redisSessionTemplate.getExpire("3", TimeUnit.SECONDS)).thenReturn(60L);
        when(redisSessionTemplate.delete("3")).thenReturn(false);

        // When
        CustomException ex = assertThrows(CustomException.class, () -> rankService.endRankGame(user));

        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMPTY_SESSION_DATA);
        assertThat(user.getCurrency()).isEqualTo(currencyBefore);
        verify(latestRankPuzzleRepository, never()).findAllByUser(any());
    }

    @Test
    void endRankGame_WhenGameEnds_ThenLeavesLastPuzzleRatingAlone() {
        // Given: the puzzle still on screen ran out of time, which says nothing about how hard it is
        UserEntity user = TestUserFactory.createTestUser("user3", 1600);
        ReflectionTestUtils.setField(user, "id", 3L);

        RankSessionData session = new RankSessionData();
        session.setStarted(true);

        when(valueOperations.get("3")).thenReturn(session);
        when(redisSessionTemplate.getExpire("3", TimeUnit.SECONDS)).thenReturn(60L);
        when(redisSessionTemplate.delete("3")).thenReturn(true);
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(latestRankPuzzleRepository.findAllByUser(user))
                .thenReturn(List.of(LatestRankPuzzle.builder()
                        .puzzleType(PuzzleType.TRAINING)
                        .puzzleId(7L)
                        .isSolved(false)
                        .build()));

        // When
        rankService.endRankGame(user);

        // Then
        verify(trainingPuzzleRepository, never()).applyRankResult(anyLong(), anyDouble(), anyDouble(), anyDouble());
        verify(communityPuzzleRepository, never()).applyRankResult(anyLong(), anyDouble(), anyDouble(), anyDouble());
    }

    // getRankArchive test
    @Test
    void getRankArchive_WhenUserNotFound_ThenThrowsCustomException() {
        // Given
        UserEntity dummy = TestUserFactory.createTestUser("noUser", 1400.0);
        ReflectionTestUtils.setField(dummy, "id", 1L);

        when(userRepository.findById(dummy.getId())).thenReturn(Optional.empty());

        // When
        CustomException ex = assertThrows(CustomException.class, () ->
                rankService.getRankArchive(dummy)
        );
        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CANNOT_FIND_USER);
    }

    @Test
    void getRankArchive_WhenUserExists_ThenReturnsArchiveList() {
        // Given
        UserEntity user = TestUserFactory.createTestUser("tester", 1500.0);
        ReflectionTestUtils.setField(user, "id", 1L);

        LatestRankPuzzle puzzle1 = LatestRankPuzzle.builder()
                .user(user)
                .boardStatus("a1a2")
                .answer("a3")
                .isSolved(true)
                .winColor(WinColor.getWinColor("WHITE"))
                .assignedAt(FIXED_INSTANT)
                .build();

        LatestRankPuzzle puzzle2 = LatestRankPuzzle.builder()
                .user(user)
                .boardStatus("b1b2")
                .answer("b3")
                .isSolved(false)
                .winColor(WinColor.getWinColor("BLACK"))
                .assignedAt(FIXED_INSTANT.plusSeconds(10))
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(latestRankPuzzleRepository.findAllByUserOrderByAssignedAtAsc(user))
                .thenReturn(List.of(puzzle1, puzzle2));

        // When
        List<RankArchive> archives = rankService.getRankArchive(user);

        // Then
        assertThat(archives).hasSize(2);
        assertThat(archives.get(0).boardStatus()).isEqualTo("a1a2");
        assertThat(archives.get(0).isSolved()).isTrue();
        assertThat(archives.get(0).winColor()).isEqualTo("WHITE");

        assertThat(archives.get(1).boardStatus()).isEqualTo("b1b2");
        assertThat(archives.get(1).isSolved()).isFalse();
        assertThat(archives.get(1).winColor()).isEqualTo("BLACK");
    }

    @Test
    void getRankArchive_WhenGameInProgress_ThenHidesThePuzzleBeingSolved() {
        // Given: one puzzle answered, the next one still on the board
        UserEntity user = TestUserFactory.createTestUser("tester", 1500.0);
        ReflectionTestUtils.setField(user, "id", 1L);

        LatestRankPuzzle answered = LatestRankPuzzle.builder()
                .id(10L)
                .user(user)
                .boardStatus("a1a2")
                .answer("a3")
                .isSolved(true)
                .winColor(WinColor.getWinColor("WHITE"))
                .assignedAt(FIXED_INSTANT)
                .build();
        LatestRankPuzzle current = LatestRankPuzzle.builder()
                .id(11L)
                .user(user)
                .boardStatus("b1b2")
                .answer("b3")
                .isSolved(false)
                .winColor(WinColor.getWinColor("BLACK"))
                .assignedAt(FIXED_INSTANT.plusSeconds(10))
                .build();

        RankSessionData session = new RankSessionData();
        session.setStarted(true);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(latestRankPuzzleRepository.findAllByUserOrderByAssignedAtAsc(user))
                .thenReturn(List.of(answered, current));
        when(valueOperations.get("1")).thenReturn(session);

        // When
        List<RankArchive> archives = rankService.getRankArchive(user);

        // Then
        assertThat(archives).extracting(RankArchive::answer).containsExactly("a3");
    }

    // getRanking test

    @Test
    void getRanking_WhenUsersHaveSameRating_ThenAssignsSameRank() {
        // Given
        UserEntity me = rankedUser(3L, "me");

        Set<ZSetOperations.TypedTuple<String>> zset = new LinkedHashSet<>();
        zset.add(new DefaultTypedTuple<>("1", 1500.0));
        zset.add(new DefaultTypedTuple<>("2", 1500.0));
        zset.add(new DefaultTypedTuple<>("3", 1300.0));

        when(redisRankingTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores("ranking:rating", 0, 99)).thenReturn(zset);
        when(userRepository.findNicknamesByIdIn(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(nickname(1L, "user1"), nickname(2L, "user2"), nickname(3L, "me")));
        when(zSetOperations.score("ranking:rating", "3")).thenReturn(1300.0);
        // Only scores strictly above mine count
        when(zSetOperations.count("ranking:rating", Math.nextUp(1300.0), Double.POSITIVE_INFINITY)).thenReturn(2L);

        // When
        GetRatingRankingResponse response = rankService.getRatingRanking(me);

        // Then
        assertThat(response.top100()).extracting(UserRatingRankInfo::rank).containsExactly(1, 1, 3);
        assertThat(response.myRatingRank().rank()).isEqualTo(3);
    }

    @Test
    void getRatingRanking_WhenRenamedOrWithdrawnSinceRefresh_ThenShowsCurrentNicknames() {
        // Given: built before user 1 changed nickname and user 2 withdrew
        UserEntity me = rankedUser(1L, "renamed");

        Set<ZSetOperations.TypedTuple<String>> zset = new LinkedHashSet<>();
        zset.add(new DefaultTypedTuple<>("1", 1600.0));
        zset.add(new DefaultTypedTuple<>("2", 1500.0));

        when(redisRankingTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores("ranking:rating", 0, 99)).thenReturn(zset);
        when(userRepository.findNicknamesByIdIn(List.of(1L, 2L))).thenReturn(List.of(nickname(1L, "renamed")));
        when(zSetOperations.score("ranking:rating", "1")).thenReturn(1600.0);
        when(zSetOperations.count("ranking:rating", Math.nextUp(1600.0), Double.POSITIVE_INFINITY)).thenReturn(0L);

        // When
        GetRatingRankingResponse response = rankService.getRatingRanking(me);

        // Then: found by id, so the new nickname still has its rank
        assertThat(response.top100()).extracting(UserRatingRankInfo::nickname).containsExactly("renamed", DELETED_USER);
        assertThat(response.myRatingRank().rank()).isEqualTo(1);
    }

    // updateRankingCache test
    @Test
    void updateRankingCache_WhenCalled_ThenStoresTopUsersInRedis() {
        // Given
        UserEntity user1 = TestUserEntityBuilder.builder().withId(1L).withRating(1400).build();
        UserEntity user2 = TestUserEntityBuilder.builder().withId(2L).withRating(1600).build();
        List<UserEntity> activeUsers = List.of(user1, user2);

        when(latestRankPuzzleRepository.findActiveUsersWithinPeriod(any())).thenReturn(activeUsers);
        when(redisRankingTemplate.opsForZSet()).thenReturn(zSetOperations);

        // When
        rankService.updateRankingCache();

        // Then
        verify(redisRankingTemplate).delete("ranking:rating:tmp");
        assertThat(storedScores("ranking:rating:tmp")).isEqualTo(Map.of("1", 1400.0, "2", 1600.0));
    }

    @Test
    void updateRankingCache_WhenRebuilt_ThenSwapsTheNewRankingInWithoutClearingIt() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().withId(1L).withRating(1400).build();
        when(latestRankPuzzleRepository.findActiveUsersWithinPeriod(any())).thenReturn(List.of(user));
        when(communityPuzzleRepository.findUsersWhoCreatedPuzzlesSince(any())).thenReturn(List.of(user));
        when(redisRankingTemplate.opsForZSet()).thenReturn(zSetOperations);

        // When
        rankService.updateRankingCache();

        // Then: readers keep seeing the old ranking until the rename
        verify(redisRankingTemplate).rename("ranking:rating:tmp", "ranking:rating");
        verify(redisRankingTemplate).rename("ranking:puzzler:tmp", "ranking:puzzler");
        verify(redisRankingTemplate, never()).delete("ranking:rating");
        verify(redisRankingTemplate, never()).delete("ranking:puzzler");
    }

    @Test
    void removeFromRankings_WhenCalled_ThenRemovesTheUserFromBothRankings() {
        // Given
        when(redisRankingTemplate.opsForZSet()).thenReturn(zSetOperations);

        // When: no transaction is active, so it runs at once
        rankService.removeFromRankings(1L);

        // Then
        verify(zSetOperations).remove("ranking:rating", "1");
        verify(zSetOperations).remove("ranking:puzzler", "1");
    }

    @Test
    void updateRankingCache_WhenNoUserIsActive_ThenClearsTheRanking() {
        // When
        rankService.updateRankingCache();

        // Then: nothing was written to the temporary keys, so there is nothing to rename
        verify(redisRankingTemplate).delete("ranking:rating");
        verify(redisRankingTemplate).delete("ranking:puzzler");
        verify(redisRankingTemplate, never()).rename(anyString(), anyString());
    }
    @Test
    void getPuzzlerRanking_WhenUsersHaveSameScore_ThenAssignsSameRank() {
        // Given
        UserEntity me = rankedUser(3L, "me");

        Set<ZSetOperations.TypedTuple<String>> zset = new LinkedHashSet<>();
        zset.add(new DefaultTypedTuple<>("1", 1500.0));
        zset.add(new DefaultTypedTuple<>("2", 1500.0));
        zset.add(new DefaultTypedTuple<>("3", 1300.0));

        when(redisRankingTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores("ranking:puzzler", 0, 99)).thenReturn(zset);
        when(userRepository.findNicknamesByIdIn(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(nickname(1L, "user1"), nickname(2L, "user2"), nickname(3L, "me")));
        when(zSetOperations.score("ranking:puzzler", "3")).thenReturn(1300.0);
        when(zSetOperations.count("ranking:puzzler", Math.nextUp(1300.0), Double.POSITIVE_INFINITY)).thenReturn(2L);

        // When
        GetPuzzlerRankingResponse response = rankService.getPuzzlerRanking(me);

        // Then
        assertThat(response.top100()).extracting(UserPuzzlerRankInfo::rank).containsExactly(1, 1, 3);
        assertThat(response.myPuzzlerRank().rank()).isEqualTo(3);
        assertThat(response.myPuzzlerRank().nickname()).isEqualTo("me");
    }

    @Test
    void getPuzzlerRanking_WhenUserIsOutsideTop100_ThenStillReturnsTheirScore() {
        // Given: the top 100 doesn't reach the user, but the full ranking does
        UserEntity me = rankedUser(2L, "me");

        Set<ZSetOperations.TypedTuple<String>> top100 = new LinkedHashSet<>();
        top100.add(new DefaultTypedTuple<>("1", 1500.0));

        when(redisRankingTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores("ranking:puzzler", 0, 99)).thenReturn(top100);
        when(userRepository.findNicknamesByIdIn(List.of(1L))).thenReturn(List.of(nickname(1L, "top")));
        when(zSetOperations.score("ranking:puzzler", "2")).thenReturn(700.0);
        when(zSetOperations.count("ranking:puzzler", Math.nextUp(700.0), Double.POSITIVE_INFINITY)).thenReturn(1L);

        // When
        GetPuzzlerRankingResponse response = rankService.getPuzzlerRanking(me);

        // Then
        assertThat(response.myPuzzlerRank().rank()).isEqualTo(2);
        assertThat(response.myPuzzlerRank().score()).isEqualTo(700.0);
    }

    @Test
    void updateRankingCache_WhenActivePuzzlerUsersExist_ThenStoreTheirScoreInRedis() {
        // Given
        UserEntity u1 = TestUserEntityBuilder.builder()
                .withId(1L)
                .withStatus(Status.getDefaultStatus())
                .withNickname("user1")
                .build();

        UserEntity u2 = TestUserEntityBuilder.builder()
                .withId(2L)
                .withStatus(Status.getDefaultStatus())
                .withNickname("user2")
                .build();

        List<UserEntity> creators = List.of(u1);
        List<UserEntity> solvers = List.of(u2);

        when(communityPuzzleRepository.findUsersWhoCreatedPuzzlesSince(any())).thenReturn(creators);
        when(userCommunityPuzzleRepository.findUsersWhoSolvedPuzzlesSince(any())).thenReturn(solvers);

        // Both users' stats come from one grouped query each: 3 solved, 1 created, 10 likes, 2 dislikes
        when(userCommunityPuzzleRepository.countSolvedByUserIds(Set.of(1L, 2L)))
                .thenReturn(List.of(solvedCount(1L, 3), solvedCount(2L, 3)));
        when(communityPuzzleRepository.sumAuthorStatsByUserIds(Set.of(1L, 2L)))
                .thenReturn(List.of(authorStats(1L, 1, 10, 2), authorStats(2L, 1, 10, 2)));

        when(redisRankingTemplate.opsForZSet()).thenReturn(zSetOperations);

        // When
        rankService.updateRankingCache();

        // Then: floor(100 * ln(4 * 2^2 * 9^3 + 1))
        verify(redisRankingTemplate).delete("ranking:puzzler:tmp");
        assertThat(storedScores("ranking:puzzler:tmp")).isEqualTo(Map.of("1", 936.0, "2", 936.0));
    }



    @Test
    void updateRankingCache_WhenUserActivityVaries_ThenHigherScoreUserRanksAbove() {
        // Given
        UserEntity user1 = TestUserEntityBuilder.builder()
                .withId(1L)
                .withStatus(Status.getDefaultStatus())
                .withNickname("user1")
                .build();

        UserEntity user2 = TestUserEntityBuilder.builder()
                .withId(2L)
                .withStatus(Status.getDefaultStatus())
                .withNickname("user2")
                .build();

        // user1: created 2, solved 1
        // user2: created 2, solved 0
        List<UserEntity> creators = List.of(user1, user2);
        List<UserEntity> solvers = List.of(user1); // only user1 has solved puzzles

        when(communityPuzzleRepository.findUsersWhoCreatedPuzzlesSince(any())).thenReturn(creators);
        when(userCommunityPuzzleRepository.findUsersWhoSolvedPuzzlesSince(any())).thenReturn(solvers);

        // Set up activity metrics; user2 solved nothing, so the grouped count leaves them out
        when(userCommunityPuzzleRepository.countSolvedByUserIds(Set.of(1L, 2L)))
                .thenReturn(List.of(solvedCount(1L, 1)));
        when(communityPuzzleRepository.sumAuthorStatsByUserIds(Set.of(1L, 2L)))
                .thenReturn(List.of(authorStats(1L, 2, 10, 3), authorStats(2L, 2, 10, 3)));

        when(redisRankingTemplate.opsForZSet()).thenReturn(zSetOperations);

        // When
        rankService.updateRankingCache();

        // Then
        Map<String, Double> scores = storedScores("ranking:puzzler:tmp");
        assertThat(scores.get("1")).isGreaterThan(scores.get("2"));
    }

    private static UserEntity rankedUser(Long id, String nickname) {
        return TestUserEntityBuilder.builder()
                .withId(id)
                .withNickname(nickname)
                .withStatus(Status.getDefaultStatus())
                .build();
    }

    private static SolvedCountProjection solvedCount(Long userId, long solved) {
        return new SolvedCountProjection() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public long getSolvedCount() {
                return solved;
            }
        };
    }

    private static AuthorStatsProjection authorStats(Long userId, long puzzles, long likes, long dislikes) {
        return new AuthorStatsProjection() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public long getPuzzleCount() {
                return puzzles;
            }

            @Override
            public long getLikeSum() {
                return likes;
            }

            @Override
            public long getDislikeSum() {
                return dislikes;
            }
        };
    }

    private static UserNicknameProjection nickname(Long id, String nickname) {
        return new UserNicknameProjection() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getNickname() {
                return nickname;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> storedScores(String key) {
        ArgumentCaptor<Set<ZSetOperations.TypedTuple<String>>> captor = ArgumentCaptor.forClass(Set.class);
        verify(zSetOperations).add(eq(key), captor.capture());
        return captor.getValue().stream()
                .collect(Collectors.toMap(ZSetOperations.TypedTuple::getValue, ZSetOperations.TypedTuple::getScore));
    }

    // getMyRating Test
    @Test
    void getMyRating_WhenUserExists_ThenReturnsRatingFromDatabase() {
        // Given: the authenticated principal holds a stale rating
        UserEntity principal = TestUserFactory.createTestUser("me", 1200.0);
        UserEntity stored = TestUserFactory.createTestUser("me", 1500.0);
        when(userRepository.findById(principal.getId())).thenReturn(Optional.of(stored));

        // When
        GetMyRatingResponse response = rankService.getMyRating(principal);

        // Then
        assertThat(response.rating()).isEqualTo(1500.0);
        verify(userRepository).findById(principal.getId());
    }

    @Test
    void getMyRating_WhenUserNotFound_ThenThrowsCustomException() {
        // Given
        UserEntity dummy = TestUserFactory.createTestUser("noname", 1500.0);
        when(userRepository.findById(dummy.getId())).thenReturn(Optional.empty());

        // When
        CustomException ex = assertThrows(CustomException.class, () ->
                rankService.getMyRating(dummy)
        );

        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CANNOT_FIND_USER);
    }
}
