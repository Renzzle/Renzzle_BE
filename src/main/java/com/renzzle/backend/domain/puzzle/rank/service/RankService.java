package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.domain.puzzle.rank.api.request.RankResultRequest;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankEndResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankResultResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankStartResponse;
import com.renzzle.backend.domain.puzzle.rank.domain.RankSessionData;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.util.EloUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankService {

    private final RedisTemplate<String, RankSessionData> redisTemplate;
    private final TrainingPuzzleRepository trainingPuzzleRepository;
    private final UserRepository userRepository;

    private static final long TTL_MINUTES = 5;
    private static final double TARGET_WIN_PROBABILITY = 0.7;
    private static final double MMR_THRESHOLD = 1500.0;
    private static final double WIN_PROBABILITY_DELTA = 0.05; // 동적 조정 기본 값

    public RankStartResponse startRankGame(UserEntity user) {
        Long userId = user.getId();
        String redisKey = String.valueOf(userId);

        //TODO: 사용자의 레에팅에 알맞는 문제를 찾는다. 사용자 레이팅에서 어느 정도의 승률의 문제를 선정해야할지 고민

        double originalMmr = user.getMmr();
        double originalRating = user.getRating();

        // 사용자의 레이팅 & 기대 승률 을 통해 적합한 문제를 가져옴
        double desiredProblemRating = EloUtil.getProblemRatingForTargetWinProbability(originalMmr, TARGET_WIN_PROBABILITY);

        TrainingPuzzle puzzle = null;
        int tolerance = 10;

        while (puzzle == null) {
            puzzle = trainingPuzzleRepository.findFirstByRatingBetweenOrderByRatingAsc(desiredProblemRating - tolerance, desiredProblemRating + tolerance);
            tolerance += 10;
        }

        double mmrPenalty = EloUtil.calculateMMRDecrease(originalRating, puzzle.getRating());
        double ratingPenalty = EloUtil.calculateRatingDecrease(originalMmr, puzzle.getRating());

        if(originalMmr >= MMR_THRESHOLD) {
            mmrPenalty *= 1.5;
            ratingPenalty *= 1.5;
        }

        double penaltyMmr = originalMmr + mmrPenalty;
        double penaltyMmrRating = originalRating + ratingPenalty;

        user.updateMmrTo(penaltyMmr);
        user.updateRatingTo(penaltyMmrRating);
        userRepository.save(user);

        RankSessionData sessionData = new RankSessionData();

        sessionData.setUserId(userId);  // sessionId == userId
        sessionData.setBoardState(puzzle.getBoardStatus());
        sessionData.setLastProblemRating(puzzle.getRating());
        sessionData.setMmrBeforePenalty(originalMmr);
        sessionData.setRatingBeforePenalty(originalRating);
        sessionData.setTargetWinProbability(TARGET_WIN_PROBABILITY);
        sessionData.setWinnerColor(puzzle.getWinColor().getName());

        redisTemplate.opsForValue().set(redisKey, sessionData, TTL_MINUTES, TimeUnit.MINUTES);

        return RankStartResponse.builder()
                .boardStatus(sessionData.getBoardState())
                .winColor(sessionData.getWinnerColor())
                .build();
    }

    @Transactional
    public RankResultResponse resultRankGame(UserEntity user, RankResultRequest request) {
        String redisKey = String.valueOf( user.getId());
        RankSessionData session = redisTemplate.opsForValue().get(redisKey);

        if (session == null){
            throw new CustomException(ErrorCode.EMPTY_SESSION_DATA);
        }

        Long currentTTL = redisTemplate.getExpire(redisKey, TimeUnit.MINUTES);
        if (currentTTL == null || currentTTL <= 0) {
            throw new CustomException(ErrorCode.INVALID_SESSION_TTL);
        }
        log.info("남은 TTL: {}분", redisTemplate.getExpire(redisKey, TimeUnit.MINUTES));

        double userBeforeMmr = session.getMmrBeforePenalty();
        double userBeforeRating = session.getRatingBeforePenalty();
        double lastProblemRating = session.getLastProblemRating();
        double WinProbability = session.getTargetWinProbability();

        double rewardMultiplier;
        double penaltyMultiplier;
        if (userBeforeMmr >= MMR_THRESHOLD) {
            rewardMultiplier = 0.5;
            penaltyMultiplier = 1.5;
        } else {
            rewardMultiplier = 1.5;
            penaltyMultiplier = 0.5;
        }
        /*
        이전 문제를 풀었다면
        다음 문제 기대 승률 조정
        배수 적용 하여 mmr % rating 값 조정 및 변수 값 업데이트
         */
        if (request.isSolved()) {
            WinProbability += WIN_PROBABILITY_DELTA;
            double mmrIncrease = EloUtil.calculateMMRIncrease(userBeforeMmr, lastProblemRating);
            double ratingIncrease = EloUtil.calculateRatingIncrease(userBeforeRating, lastProblemRating);
            mmrIncrease = Math.round(mmrIncrease * rewardMultiplier);
            ratingIncrease = Math.round(ratingIncrease * rewardMultiplier);

            user.updateMmrTo(userBeforeMmr + mmrIncrease);
            user.updateRatingTo(userBeforeRating + ratingIncrease);

            userBeforeMmr = userBeforeMmr + mmrIncrease;
            userBeforeRating = userBeforeRating + ratingIncrease;
        } else {
            WinProbability -= WIN_PROBABILITY_DELTA;
            double mmrDecrease = EloUtil.calculateMMRDecrease(userBeforeMmr, lastProblemRating);
            double ratingDecrease = EloUtil.calculateRatingDecrease(userBeforeRating, lastProblemRating);

            mmrDecrease = Math.round(mmrDecrease * penaltyMultiplier);
            ratingDecrease = Math.round(ratingDecrease * penaltyMultiplier);

            user.updateMmrTo(userBeforeMmr + mmrDecrease);
            user.updateRatingTo(userBeforeRating + ratingDecrease);

            userBeforeMmr = userBeforeMmr + mmrDecrease;
            userBeforeRating = userBeforeRating + ratingDecrease;
        }

        // 사용자의 레이팅 & 기대 승률 을 통해 적합한 문제를 가져옴
        double desiredProblemRating = EloUtil.getProblemRatingForTargetWinProbability(userBeforeMmr, WinProbability);

        TrainingPuzzle puzzle = null;
        int tolerance = 10;

        while (puzzle == null) {
            puzzle = trainingPuzzleRepository.findFirstByRatingBetweenOrderByRatingAsc(desiredProblemRating - tolerance, desiredProblemRating + tolerance);
            tolerance += 10;
        }

        double ratingPenalty = EloUtil.calculateRatingDecrease(userBeforeRating, puzzle.getRating());
        double mmrPenalty = EloUtil.calculateMMRDecrease(userBeforeMmr, puzzle.getRating());

        if(userBeforeMmr >= MMR_THRESHOLD) {
            mmrPenalty *= 1.5;
            ratingPenalty *= 1.5;
        }

        double penaltyMmr = userBeforeMmr + mmrPenalty;
        double penaltyRating = userBeforeRating + ratingPenalty;

        user.updateMmrTo(penaltyMmr);
        user.updateRatingTo(penaltyRating);

        userRepository.save(user);

        // TODO: 이전 문제 저장. 이 때 문제의 풀이 여부도 저장한다.
        /*
        사용자가 랭킹전에서 마주한 문제는 어디에 저장해둬야하는거지?
         */
//        trainingPuzzleRepository.saveProblemResult(session);


        // 세션 갱신: 새 문제 정보 반영 (boardState와 winnerColor는 예시)
        session.setBoardState(puzzle.getBoardStatus());
        session.setLastProblemRating(puzzle.getRating());
        session.setWinnerColor(puzzle.getWinColor().getName());
        session.setMmrBeforePenalty(userBeforeMmr);
        session.setRatingBeforePenalty(userBeforeRating);
        session.setTargetWinProbability(WinProbability);

        redisTemplate.opsForValue().set(redisKey, session, currentTTL, TimeUnit.MINUTES);

        return RankResultResponse.builder()
                .boardStatus(puzzle.getBoardStatus())
                .winColor(puzzle.getWinColor().getName())
                .build();
    }

    public RankEndResponse endRankGame(UserEntity user) {
        String redisKey = String.valueOf( user.getId());
        RankSessionData session = redisTemplate.opsForValue().get(redisKey);
        if (session != null) {
            redisTemplate.delete(redisKey);
        }
        return RankEndResponse.builder()
                .rating(user.getRating())
                .build();
    }
}
