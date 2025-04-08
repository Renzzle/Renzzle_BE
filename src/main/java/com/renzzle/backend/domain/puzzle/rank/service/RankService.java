package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.domain.puzzle.rank.api.request.RankResultRequest;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankEndResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankResultResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankStartResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    @Value("${rank.session.ttl}")
    private long sessionTTLSeconds;

    public RankStartResponse startRankGame(UserEntity userData) {
        UserEntity user = userRepository.findById(userData.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));
        Long userId = user.getId();
        String redisKey = String.valueOf(userId);

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

        String redisKey = String.valueOf( user.getId());
        RankSessionData session = redisTemplate.opsForValue().get(redisKey);

        if (session == null){
            throw new CustomException(ErrorCode.EMPTY_SESSION_DATA);
        }

        if(!session.isStarted()){
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
}
