package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.domain.puzzle.rank.api.request.RankResultRequest;
import com.renzzle.backend.domain.puzzle.rank.api.response.*;
import com.renzzle.backend.domain.puzzle.rank.dao.LatestRankPuzzleRepository;
import com.renzzle.backend.domain.puzzle.rank.domain.LatestRankPuzzle;
import com.renzzle.backend.domain.puzzle.rank.domain.RankSessionData;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.util.ELOUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.renzzle.backend.global.util.ELOUtil.TARGET_WIN_PROBABILITY;
import static com.renzzle.backend.global.util.ELOUtil.WIN_PROBABILITY_DELTA;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankService {

    private final RedisTemplate<String, RankSessionData> redisTemplate;
    private final TrainingPuzzleRepository trainingPuzzleRepository;
    private final UserRepository userRepository;
    private final LatestRankPuzzleRepository latestRankPuzzleRepository;
    private final Clock clock;
    private final RedisTemplate<String, Object> redisRankingTemplate;

    @Value("${rank.session.ttl}")
    private long sessionTTLSeconds;

    public RankStartResponse startRankGame(UserEntity userData) {
        UserEntity user = userRepository.findById(userData.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));
        Long userId = user.getId();
        String redisKey = String.valueOf(userId);

        List<LatestRankPuzzle> existingPuzzles = latestRankPuzzleRepository.findAllByUser(user);
        if (!existingPuzzles.isEmpty()) {
            latestRankPuzzleRepository.deleteAll(existingPuzzles);
        }

        double originalMmr = user.getMmr();
        double originalRating = user.getRating();

        TrainingPuzzle puzzle = getNextPuzzle(originalMmr, TARGET_WIN_PROBABILITY, user);

        LatestRankPuzzle latestPuzzle = LatestRankPuzzle.builder()
                .user(user)
                .boardStatus(puzzle.getBoardStatus())
                .answer(puzzle.getAnswer())
                .isSolved(false)
                .assignedAt(clock.instant())
                .winColor(puzzle.getWinColor())
                .build();

        latestRankPuzzleRepository.save(latestPuzzle);

        double mmrPenalty = ELOUtil.calculateMMRDecrease(originalRating, puzzle.getRating());
        double ratingPenalty = ELOUtil.calculateRatingDecrease(originalMmr, puzzle.getRating());

        user.updateMmrTo(originalMmr + mmrPenalty);
        user.updateRatingTo(originalRating + ratingPenalty);
        userRepository.save(user);

        RankSessionData sessionData = new RankSessionData();

        sessionData.setUserId(userId);
        sessionData.setBoardState(puzzle.getBoardStatus());
        sessionData.setLastProblemRating(puzzle.getRating());
        sessionData.setMmrBeforePenalty(originalMmr);
        sessionData.setRatingBeforePenalty(originalRating);
        sessionData.setTargetWinProbability(TARGET_WIN_PROBABILITY);
        sessionData.setWinnerColor(puzzle.getWinColor().getName());
        sessionData.setStarted(true);

        redisTemplate.opsForValue().set(redisKey, sessionData, sessionTTLSeconds, TimeUnit.SECONDS);

        return RankStartResponse.builder()
                .boardStatus(sessionData.getBoardState())
                .winColor(sessionData.getWinnerColor())
                .build();
    }

    @Transactional
    public RankResultResponse resultRankGame(UserEntity userData, RankResultRequest request) {

        UserEntity user = userRepository.findById(userData.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));

        String redisKey = String.valueOf(user.getId());
        RankSessionData session = redisTemplate.opsForValue().get(redisKey);

        if (session == null) {
            throw new CustomException(ErrorCode.EMPTY_SESSION_DATA);
        }

        if (!session.isStarted()) {
            throw new CustomException(ErrorCode.EMPTY_SESSION_DATA);
        }

        Long currentTTL = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        if (currentTTL == null || currentTTL <= 0) {
            throw new CustomException(ErrorCode.INVALID_SESSION_TTL);
        }

        // 이전 문제 조회 및 풀이 여부 업데이트
        LatestRankPuzzle previousPuzzle = latestRankPuzzleRepository
                .findTopByUserOrderByAssignedAtDesc(user)
                .orElseThrow(() -> new CustomException(ErrorCode.PUZZLE_NOT_FOUND));

        previousPuzzle.solvedUpdate(request.isSolved());

        double userBeforeMmr = session.getMmrBeforePenalty();
        double userBeforeRating = session.getRatingBeforePenalty();
        double lastProblemRating = session.getLastProblemRating();
        double WinProbability = session.getTargetWinProbability();
        /*
        이전 문제를 풀었다면
        다음 문제 기대 승률 조정
        배수 적용 하여 mmr % rating 값 조정 및 변수 값 업데이트
         */
        if (request.isSolved()) {
            WinProbability += WIN_PROBABILITY_DELTA;

            double mmrIncrease = ELOUtil.calculateMMRIncrease(userBeforeMmr, lastProblemRating);
            double ratingIncrease = ELOUtil.calculateRatingIncrease(userBeforeRating, lastProblemRating);

            user.updateMmrTo(userBeforeMmr + mmrIncrease);
            user.updateRatingTo(userBeforeRating + ratingIncrease);

            userBeforeMmr = userBeforeMmr + mmrIncrease;
            userBeforeRating = userBeforeRating + ratingIncrease;
        } else {
            WinProbability -= WIN_PROBABILITY_DELTA;
            double mmrDecrease = ELOUtil.calculateMMRDecrease(userBeforeMmr, lastProblemRating);
            double ratingDecrease = ELOUtil.calculateRatingDecrease(userBeforeRating, lastProblemRating);

            user.updateMmrTo(userBeforeMmr + mmrDecrease);
            user.updateRatingTo(userBeforeRating + ratingDecrease);

            userBeforeMmr = userBeforeMmr + mmrDecrease;
            userBeforeRating = userBeforeRating + ratingDecrease;
        }

        // 사용자의 레이팅 & 기대 승률 을 통해 적합한 문제를 가져옴
        TrainingPuzzle puzzle = getNextPuzzle(userBeforeMmr, WinProbability, user);

        double ratingPenalty = ELOUtil.calculateRatingDecrease(userBeforeRating, puzzle.getRating());
        double mmrPenalty = ELOUtil.calculateMMRDecrease(userBeforeMmr, puzzle.getRating());

        user.updateMmrTo(userBeforeMmr + mmrPenalty);
        user.updateRatingTo(userBeforeRating + ratingPenalty);

        userRepository.save(user);

        LatestRankPuzzle nextPuzzle = LatestRankPuzzle.builder()
                .user(user)
                .boardStatus(puzzle.getBoardStatus())
                .answer(puzzle.getAnswer())
                .isSolved(false)
                .assignedAt(clock.instant())
                .winColor(puzzle.getWinColor())
                .build();

        latestRankPuzzleRepository.save(nextPuzzle);

        session.setBoardState(puzzle.getBoardStatus());
        session.setLastProblemRating(puzzle.getRating());
        session.setWinnerColor(puzzle.getWinColor().getName());
        session.setMmrBeforePenalty(userBeforeMmr);
        session.setRatingBeforePenalty(userBeforeRating);
        session.setTargetWinProbability(WinProbability);

        redisTemplate.opsForValue().set(redisKey, session, currentTTL, TimeUnit.SECONDS);

        return RankResultResponse.builder()
                .boardStatus(puzzle.getBoardStatus())
                .winColor(puzzle.getWinColor().getName())
                .build();
    }

    public RankEndResponse endRankGame(UserEntity userData) {
        String redisKey = String.valueOf(userData.getId());
        RankSessionData session = redisTemplate.opsForValue().get(redisKey);

        if (session == null) {
            throw new CustomException(ErrorCode.EMPTY_SESSION_DATA);
        }
        if (!session.isStarted()) {
            throw new CustomException(ErrorCode.IS_NOT_STARTED);
        }

        redisTemplate.delete(redisKey);

        return RankEndResponse.builder()
                .rating(userData.getRating())
                .build();
    }

    TrainingPuzzle getNextPuzzle(double originalMmr, double targetWinProbability, UserEntity user) {
        // 사용자의 레이팅 & 기대 승률 을 통해 적합한 문제를 가져옴
        double desiredRating = ELOUtil.getProblemRatingForTargetWinProbability(originalMmr, targetWinProbability);

        int tolerance = 10;
        List<TrainingPuzzle> candidates = Collections.emptyList();

        while (candidates.isEmpty()) {
            double min = desiredRating - tolerance;
            double max = desiredRating + tolerance;

            candidates = trainingPuzzleRepository.findAvailablePuzzlesForUser(min, max, user);
            tolerance += 10;
        }

        // 랜덤하게 하나 선택
        Collections.shuffle(candidates);
        return candidates.get(0);
    }

    @Transactional
    public List<RankArchive> getRankArchive(UserEntity userData) {

        UserEntity user = userRepository.findById(userData.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));

        List<LatestRankPuzzle> puzzles = latestRankPuzzleRepository.findAllByUserOrderByAssignedAtAsc(user);

        List<RankArchive> archives = puzzles.stream()
                .map(puzzle -> RankArchive.builder()
                        .boardStatus(puzzle.getBoardStatus())
                        .answer(puzzle.getAnswer())
                        .isSolved(puzzle.getIsSolved())
                        .winColor(puzzle.getWinColor().getName())
                        .build())
                .collect(Collectors.toList());

        return archives;
    }

    @Transactional(readOnly = true)
    public GetRankingResponse getRanking(UserEntity userData) {

        String rankingKey = "user:ranking";

        Set<ZSetOperations.TypedTuple<Object>> top100Set =
                Optional.ofNullable(redisRankingTemplate.opsForZSet()
                                .reverseRangeWithScores(rankingKey, 0, 99))
                        .orElse(Collections.emptySet());

        List<UserRankInfo> top100 = new ArrayList<>();
        int currentRank = 1;
        double lastScore = -1;
        int rankCounter = 0;

        for (ZSetOperations.TypedTuple<Object> tuple : top100Set) {
            UserRankInfo info = (UserRankInfo) tuple.getValue();
            double score = Objects.requireNonNull(info).rating();
            rankCounter++;

            if (Double.compare(score, lastScore) != 0) {
                currentRank = rankCounter;
                lastScore = score;
            }

            top100.add(UserRankInfo.builder()
                    .rank(currentRank)
                    .nickname(info.nickname())
                    .rating(info.rating())
                    .build());
        }

        // 내 정보 랭킹 계산 (전체 사용자 기반 공동 순위)
        Set<ZSetOperations.TypedTuple<Object>> allUsersSet =
                Optional.ofNullable(redisRankingTemplate.opsForZSet()
                                .reverseRangeWithScores(rankingKey, 0, -1))
                        .orElse(Collections.emptySet());

        int tieAwareRank = 1;
        double myRating = userData.getRating();
        lastScore = -1;
        rankCounter = 0;
        int myFinalRank = -1;

        for (ZSetOperations.TypedTuple<Object> tuple : allUsersSet) {
            Double score = tuple.getScore();
            if (score == null) continue;
            rankCounter++;

            if (Double.compare(score, lastScore) != 0) {
                tieAwareRank = rankCounter;
                lastScore = score;
            }

            if (Double.compare(score, myRating) == 0) {
                myFinalRank = tieAwareRank;
                break;
            }
        }

//        // 내 정보 랭킹 계산
//        UserRankInfo myInfo = UserRankInfo.builder()
//                .rank(0)
//                .nickname(userData.getNickname())
//                .rating(userData.getRating())
//                .build();
//
//        Long myRankRaw = redisRankingTemplate.opsForZSet().reverseRank(rankingKey, myInfo);
//
//        int finalRank = -1;
//
//        if (myRankRaw != null) {
//            // 공동 순위 계산
//            Set<ZSetOperations.TypedTuple<Object>> allUsersSet = redisRankingTemplate.opsForZSet()
//                    .reverseRangeWithScores(rankingKey, 0, -1);
//
//            int tieAwareRank = 1;
//            double myRating = userData.getRating();
//
//            Set<Double> uniqueScores = Objects.requireNonNull(allUsersSet).stream()
//                    .map(ZSetOperations.TypedTuple::getScore)
//                    .filter(Objects::nonNull)
//                    .collect(Collectors.toCollection(LinkedHashSet::new));
//
//            for (Double score : uniqueScores) {
//                if (Double.compare(score, myRating) > 0) {
//                    tieAwareRank++;
//                } else if (Double.compare(score, myRating) == 0) {
//                    break;
//                }
//            }
//
//            finalRank = tieAwareRank;
//        }

        UserRankInfo myRankInfo = UserRankInfo.builder()
                .rank(myFinalRank)
                .nickname(userData.getNickname())
                .rating(userData.getRating())
                .build();

        return GetRankingResponse.builder()
                .top100(top100)
                .myRank(myRankInfo)
                .build();
    }

    @Scheduled(fixedRate = 1000 * 60 * 60) // 60분마다 실행
    public void updateRankingCache() {
        String rankingKey = "user:ranking";

        redisRankingTemplate.delete(rankingKey);

        List<UserEntity> allUsers = userRepository.findAll();
        for (UserEntity user : allUsers) {
            UserRankInfo info = UserRankInfo.builder()
                    .rank(0)
                    .nickname(user.getNickname())
                    .rating(user.getRating())
                    .build();

            redisRankingTemplate.opsForZSet().add(rankingKey, info, user.getRating());
        }
    }

}
