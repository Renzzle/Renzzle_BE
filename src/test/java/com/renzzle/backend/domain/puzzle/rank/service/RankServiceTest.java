package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.rank.api.request.RankResultRequest;
import com.renzzle.backend.domain.puzzle.rank.api.response.*;
import com.renzzle.backend.domain.puzzle.rank.dao.LatestRankPuzzleRepository;
import com.renzzle.backend.domain.puzzle.rank.domain.LatestRankPuzzle;
import com.renzzle.backend.domain.puzzle.rank.domain.RankSessionData;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RankServiceTest {
    private RankService rankService;
    @Mock
    private RedisTemplate<String, Object> redisRankingTemplate;
    @Mock
    private ZSetOperations<String, Object> zSetOperations;
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
    private Clock clock;
    @BeforeEach
    void setup() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2025-01-01T00:00:00Z"));
        rankService = new RankService(
                redisSessionTemplate,
                trainingPuzzleRepository,
                communityPuzzleRepository,
                userRepository,
                latestRankPuzzleRepository,
                clock,
                redisRankingTemplate
        );

        lenient().when(redisSessionTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // startRankGame Test
    @Test
    void startRankGame_WhenUserNotFound_ThenThrowsCustomException() {
        // Given
        UserEntity dummy = TestUserFactory.createTestUser("noname", 1500.0);
        when(userRepository.findById(dummy.getId())).thenReturn(Optional.empty());

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
        ReflectionTestUtils.setField(user, "id", 1L); // ID 강제 주입

        TrainingPuzzle puzzle = TrainingPuzzle.builder()
                .boardStatus("a1a2")
                .answer("a3")
                .rating(1400)
                .winColor(WinColor.getWinColor("BLACK"))
                .depth(3)
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
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

    // resultRankGame test

    @Test
    void resultRankGame_WhenSessionIsNull_ThenThrowsEmptySession() {
        //Given
        UserEntity user = TestUserFactory.createTestUser("u1", 1500);
        ReflectionTestUtils.setField(user, "id", 1L);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.get("1")).thenReturn(null);

        RankResultRequest request = new RankResultRequest(true);

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

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.get("1")).thenReturn(session);

        RankResultRequest request = new RankResultRequest(true);

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

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.get("1")).thenReturn(session);
        when(redisSessionTemplate.getExpire("1", TimeUnit.SECONDS)).thenReturn(0L); // or null

        RankResultRequest request = new RankResultRequest(true);
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
        session.setMmrBeforePenalty(1500);
        session.setRatingBeforePenalty(1500);
        session.setLastProblemRating(1400);
        session.setTargetWinProbability(0.7);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.get("1")).thenReturn(session);
        when(redisSessionTemplate.getExpire("1", TimeUnit.SECONDS)).thenReturn(60L);
        when(latestRankPuzzleRepository.findTopByUserOrderByAssignedAtDesc(user)).thenReturn(Optional.empty());
        // When
        RankResultRequest request = new RankResultRequest(true);

        CustomException ex = assertThrows(CustomException.class, () -> rankService.resultRankGame(user, request));
        // Then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PUZZLE_NOT_FOUND);
    }

    @Test
    void resultRankGame_WhenSolvedCorrectly_ThenReturnsNextPuzzle() {
        // Given
        UserEntity user = TestUserFactory.createTestUser("u1", 1500);
        ReflectionTestUtils.setField(user, "id", 1L);

        RankSessionData session = new RankSessionData();
        session.setStarted(true);
        session.setMmrBeforePenalty(1500);
        session.setRatingBeforePenalty(1500);
        session.setLastProblemRating(1400);
        session.setTargetWinProbability(0.7);

        // 기존 문제(이전 라운드 문제)
        LatestRankPuzzle previous = LatestRankPuzzle.builder()
                .user(user)
                .boardStatus("a1a2")
                .answer("a3")
                .isSolved(false)
                .assignedAt(clock.instant())
                .winColor(WinColor.getWinColor("WHITE"))
                .build();

        // 다음 문제 후보 (TrainingPuzzle)
        TrainingPuzzle candidatePuzzle = TrainingPuzzle.builder()
                .boardStatus("nextBoard")
                .answer("nextAnswer")
                .depth(3)
                .rating(1450)
                .winColor(WinColor.getWinColor("BLACK"))
                .build();


        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.get("1")).thenReturn(session);
        when(redisSessionTemplate.getExpire("1", TimeUnit.SECONDS)).thenReturn(120L);
        when(latestRankPuzzleRepository.findTopByUserOrderByAssignedAtDesc(user)).thenReturn(Optional.of(previous));
        when(trainingPuzzleRepository.findAvailableTrainingPuzzlesSortedByRating(user)).thenReturn(List.of(candidatePuzzle));
        when(communityPuzzleRepository.findAvailableCommunityPuzzlesSortedByRating(user)).thenReturn(Collections.emptyList());
        when(clock.instant()).thenReturn(Instant.parse("2025-01-01T00:00:00Z"));

        RankResultRequest request = new RankResultRequest(true);
        RankResultResponse response = rankService.resultRankGame(user, request);

        // Then
        assertThat(response.boardStatus()).isEqualTo("nextBoard");
        assertThat(response.winColor()).isEqualTo("BLACK");

        verify(redisSessionTemplate.opsForValue())
                .set(eq("1"), any(RankSessionData.class), anyLong(), eq(TimeUnit.SECONDS));
    }

    // endRankGame test

    @Test
    void endRankGame_WhenSessionIsNull_ThenThrowsException() {
        // Given
        UserEntity user = TestUserFactory.createTestUser("user1", 1500);
        ReflectionTestUtils.setField(user, "id", 1L);

        when(valueOperations.get("1")).thenReturn(null); // 세션 없음
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
        session.setStarted(false); // 시작되지 않은 세션

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
        // When
        RankEndResponse response = rankService.endRankGame(user);
        // Then
        assertThat(response.rating()).isEqualTo(1600);
        verify(redisSessionTemplate).delete("3");
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
                .assignedAt(Instant.now())
                .build();

        LatestRankPuzzle puzzle2 = LatestRankPuzzle.builder()
                .user(user)
                .boardStatus("b1b2")
                .answer("b3")
                .isSolved(false)
                .winColor(WinColor.getWinColor("BLACK"))
                .assignedAt(Instant.now().plusSeconds(10))
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

    // getRanking test

    @Test
    void getRanking_WhenUsersHaveSameRating_ThenAssignsSameRank() {
        // Given
        UserEntity user = TestUserFactory.createTestUser("me", 1300.0);

        UserRankInfo info1 = UserRankInfo.builder().nickname("user1").rating(1500).rank(0).build();
        UserRankInfo info2 = UserRankInfo.builder().nickname("user2").rating(1500).rank(0).build();
        UserRankInfo myInfo = UserRankInfo.builder().nickname("me").rating(1300).rank(0).build();

        Set<ZSetOperations.TypedTuple<Object>> zset = new LinkedHashSet<>();
        zset.add(new DefaultTypedTuple<>(info1, 1500.0));
        zset.add(new DefaultTypedTuple<>(info2, 1500.0));
        zset.add(new DefaultTypedTuple<>(myInfo, 1300.0));

        when(zSetOperations.reverseRangeWithScores("user:ranking", 0, 99)).thenReturn(zset);
        when(zSetOperations.reverseRangeWithScores("user:ranking", 0, -1)).thenReturn(zset);
        when(redisRankingTemplate.opsForZSet()).thenReturn(zSetOperations);

        // When
        GetRankingResponse response = rankService.getRanking(user);

        // Then
        assertThat(response.top100()).hasSize(3);
        assertThat(response.top100().get(0).rank()).isEqualTo(1);
        assertThat(response.top100().get(1).rank()).isEqualTo(1);
        assertThat(response.top100().get(2).rank()).isEqualTo(3);
        assertThat(response.myRank().rank()).isEqualTo(3);
    }

    // updateRankingCache test
    @Test
    void updateRankingCache_WhenCalled_ThenStoresTopUsersInRedis() {
        // Given
        UserEntity user1 = TestUserFactory.createTestUser("u1", 1400);
        UserEntity user2 = TestUserFactory.createTestUser("u2", 1600);
        List<UserEntity> activeUsers = List.of(user1, user2);

        when(latestRankPuzzleRepository.findActiveUsersWithinPeriod(any())).thenReturn(activeUsers);
        when(redisRankingTemplate.opsForZSet()).thenReturn(zSetOperations);

        // When
        rankService.updateRankingCache();

        // Then
        verify(redisRankingTemplate).delete("user:ranking");
        verify(latestRankPuzzleRepository).findActiveUsersWithinPeriod(any());

        verify(zSetOperations, times(2)).add(eq("user:ranking"), any(UserRankInfo.class), anyDouble());

        verify(zSetOperations).add(eq("user:ranking"),
                argThat(info -> ((UserRankInfo) info).nickname().equals("u1")),
                eq(1400.0));
        verify(zSetOperations).add(eq("user:ranking"),
                argThat(info -> ((UserRankInfo) info).nickname().equals("u2")),
                eq(1600.0));
    }
}
