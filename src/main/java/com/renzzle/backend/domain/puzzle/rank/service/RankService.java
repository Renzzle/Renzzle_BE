package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.rank.api.request.RankResultRequest;
import com.renzzle.backend.domain.puzzle.rank.api.response.*;
import com.renzzle.backend.domain.puzzle.rank.dao.LatestRankPuzzleRepository;
import com.renzzle.backend.domain.puzzle.rank.domain.LatestRankPuzzle;
import com.renzzle.backend.domain.puzzle.rank.domain.RankSessionData;
import com.renzzle.backend.domain.puzzle.rank.service.dto.NextPuzzleResult;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.domain.puzzle.shared.util.ELOUtils;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.renzzle.backend.domain.puzzle.shared.util.ELOUtils.TARGET_WIN_PROBABILITY;
import static com.renzzle.backend.domain.puzzle.shared.util.ELOUtils.WIN_PROBABILITY_DELTA;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankService {

    private final RedisTemplate<String, RankSessionData> redisTemplate;
    private final TrainingPuzzleRepository trainingPuzzleRepository;
    private final CommunityPuzzleRepository communityPuzzleRepository;
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

        NextPuzzleResult puzzleResult = getNextPuzzle(originalMmr, TARGET_WIN_PROBABILITY, user);
        LatestRankPuzzle latestPuzzle = puzzleResult.latestPuzzle();
        double puzzleRating = puzzleResult.rating();

        latestRankPuzzleRepository.save(latestPuzzle);

        double mmrPenalty = ELOUtils.calculateMMRDecrease(originalRating, puzzleRating);
        double ratingPenalty = ELOUtils.calculateRatingDecrease(originalMmr, puzzleRating);

        user.updateMmrTo(originalMmr + mmrPenalty);
        user.updateRatingTo(originalRating + ratingPenalty);
        userRepository.save(user);

        RankSessionData sessionData = new RankSessionData();

        sessionData.setUserId(userId);
        sessionData.setBoardState(latestPuzzle.getBoardStatus());
        sessionData.setLastProblemRating(puzzleRating);
        sessionData.setMmrBeforePenalty(originalMmr);
        sessionData.setRatingBeforePenalty(originalRating);
        sessionData.setTargetWinProbability(TARGET_WIN_PROBABILITY);
        sessionData.setWinnerColor(latestPuzzle.getWinColor().getName());
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

            double mmrIncrease = ELOUtils.calculateMMRIncrease(userBeforeMmr, lastProblemRating);
            double ratingIncrease = ELOUtils.calculateRatingIncrease(userBeforeRating, lastProblemRating);

            user.updateMmrTo(userBeforeMmr + mmrIncrease);
            user.updateRatingTo(userBeforeRating + ratingIncrease);

            userBeforeMmr = userBeforeMmr + mmrIncrease;
            userBeforeRating = userBeforeRating + ratingIncrease;
        } else {
            WinProbability -= WIN_PROBABILITY_DELTA;
            double mmrDecrease = ELOUtils.calculateMMRDecrease(userBeforeMmr, lastProblemRating);
            double ratingDecrease = ELOUtils.calculateRatingDecrease(userBeforeRating, lastProblemRating);

            user.updateMmrTo(userBeforeMmr + mmrDecrease);
            user.updateRatingTo(userBeforeRating + ratingDecrease);

            userBeforeMmr = userBeforeMmr + mmrDecrease;
            userBeforeRating = userBeforeRating + ratingDecrease;
        }

        // 사용자의 레이팅 & 기대 승률 을 통해 적합한 문제를 가져옴
        NextPuzzleResult puzzleResult = getNextPuzzle(userBeforeMmr, WinProbability, user);
        LatestRankPuzzle latestPuzzle = puzzleResult.latestPuzzle();
        double puzzleRating = puzzleResult.rating();

        double ratingPenalty = ELOUtils.calculateRatingDecrease(userBeforeRating, puzzleRating);
        double mmrPenalty = ELOUtils.calculateMMRDecrease(userBeforeMmr, puzzleRating);

        user.updateMmrTo(userBeforeMmr + mmrPenalty);
        user.updateRatingTo(userBeforeRating + ratingPenalty);

        userRepository.save(user);

        LatestRankPuzzle nextPuzzle = LatestRankPuzzle.builder()
                .user(user)
                .boardStatus(latestPuzzle.getBoardStatus())
                .answer(latestPuzzle.getAnswer())
                .isSolved(false)
                .assignedAt(clock.instant())
                .winColor(latestPuzzle.getWinColor())
                .build();

        latestRankPuzzleRepository.save(nextPuzzle);

        session.setBoardState(latestPuzzle.getBoardStatus());
        session.setLastProblemRating(puzzleRating);
        session.setWinnerColor(latestPuzzle.getWinColor().getName());
        session.setMmrBeforePenalty(userBeforeMmr);
        session.setRatingBeforePenalty(userBeforeRating);
        session.setTargetWinProbability(WinProbability);

        redisTemplate.opsForValue().set(redisKey, session, currentTTL, TimeUnit.SECONDS);

        return RankResultResponse.builder()
                .boardStatus(latestPuzzle.getBoardStatus())
                .winColor(latestPuzzle.getWinColor().getName())
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

    NextPuzzleResult getNextPuzzle(double originalMmr, double targetWinProbability, UserEntity user) {

        /*
            사용자의 레이팅 & 기대 승률 을 통해 적합한 문제를 가져옴
            문제들을
            windowSize 만큼 각 퍼즐에서 선정 후 이들을 섞은 후 이 중 하나의 문제를 선택하여 다음 문제로 사용
        */
        double desiredRating = ELOUtils.getProblemRatingForTargetWinProbability(originalMmr, targetWinProbability);
        int windowSize = 5;

        // 각 퍼즐 후보군 가져오기 (레이팅 기준 정렬)
        List<TrainingPuzzle> trainingPuzzles =
                trainingPuzzleRepository.findAvailableTrainingPuzzlesSortedByRating(user);
        List<CommunityPuzzle> communityPuzzles =
                communityPuzzleRepository.findAvailableCommunityPuzzlesSortedByRating(user);

        // 슬라이싱 유틸
        List<TrainingPuzzle> selectedTrainings = pickNearByWindow(trainingPuzzles, desiredRating, windowSize);
        List<CommunityPuzzle> selectedCommunities = pickNearByWindow(communityPuzzles, desiredRating, windowSize);

        List<Object> allCandidates = new ArrayList<>();
        allCandidates.addAll(selectedTrainings);
        allCandidates.addAll(selectedCommunities);

        if (allCandidates.isEmpty()) {
            throw new CustomException(ErrorCode.CANNOT_FIND_PUZZLE);
        }

        Collections.shuffle(allCandidates);

        Object selected = allCandidates.get(0);

        if (selected instanceof TrainingPuzzle puzzle) {
            LatestRankPuzzle latest = LatestRankPuzzle.builder()
                    .user(user)
                    .boardStatus(puzzle.getBoardStatus())
                    .answer(puzzle.getAnswer())
                    .isSolved(false)
                    .assignedAt(clock.instant())
                    .winColor(puzzle.getWinColor())
                    .build();

            return new NextPuzzleResult(latest, puzzle.getRating());
        }

        if (selected instanceof CommunityPuzzle puzzle) {
            LatestRankPuzzle latest = LatestRankPuzzle.builder()
                    .user(user)
                    .boardStatus(puzzle.getBoardStatus())
                    .answer(puzzle.getAnswer())
                    .isSolved(false)
                    .assignedAt(clock.instant())
                    .winColor(puzzle.getWinColor())
                    .build();

            return new NextPuzzleResult(latest, puzzle.getRating());
        }
        throw new CustomException(ErrorCode.INVALID_PUZZLE_TYPE);
    }

    private <T> List<T> pickNearByWindow(List<T> sorted, double targetRating, int windowSize) {
        /*
            각 퍼즐에서 windowSize 만큼의 문제들을 선택
            단, 기준 문제의 레이팅에서 maxDiff 이상의 오차를 가진 문제는 선정하지 않음.
            배열의 끝에 다다르면 그 쪽으로는 더 이상 진행하지 않음
        */
        if (sorted.isEmpty()) return Collections.emptyList();

        double maxDiff = 200.0; // 레이팅 허용 오차

        // 가장 가까운 퍼즐 위치 탐색
        int centerIndex = 0;
        double closest = Double.MAX_VALUE;
        for (int i = 0; i < sorted.size(); i++) {
            double rating = getRating(sorted.get(i));
            double diff = Math.abs(rating - targetRating);
            if (diff < closest) {
                closest = diff;
                centerIndex = i;
            }
        }

        List<T> result = new ArrayList<>();
        int left = centerIndex, right = centerIndex + 1;

        while (result.size() < windowSize && (left >= 0 || right < sorted.size())) {
            boolean picked = false;

            if (left >= 0) {
                double ratingLeft = getRating(sorted.get(left));
                if (Math.abs(ratingLeft - targetRating) <= maxDiff) {
                    result.add(sorted.get(left));
                    picked = true;
                }
                left--;
            }

            if (result.size() >= windowSize) break;

            if (right < sorted.size()) {
                double ratingRight = getRating(sorted.get(right));
                if (Math.abs(ratingRight - targetRating) <= maxDiff) {
                    result.add(sorted.get(right));
                    picked = true;
                }
                right++;
            }

            if (!picked) break; // 양쪽 다 더 이상 유효 후보가 없음
        }

        return result;
    }

    // 퍼즐 rating 추출 유틸
    private double getRating(Object obj) {
        if (obj instanceof TrainingPuzzle tp) return tp.getRating();
        if (obj instanceof CommunityPuzzle cp) return cp.getRating();
        return 0.0;
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

        Instant oneMonthAgo = Instant.now(clock).minus(30, ChronoUnit.DAYS);
        List<UserEntity> activeUsers = latestRankPuzzleRepository.findActiveUsersWithinPeriod(oneMonthAgo);

        redisRankingTemplate.delete(rankingKey);

        for (UserEntity user : activeUsers) {
            UserRankInfo info = UserRankInfo.builder()
                    .rank(0)
                    .nickname(user.getNickname())
                    .rating(user.getRating())
                    .build();

            redisRankingTemplate.opsForZSet().add(rankingKey, info, user.getRating());
        }
    }
}
