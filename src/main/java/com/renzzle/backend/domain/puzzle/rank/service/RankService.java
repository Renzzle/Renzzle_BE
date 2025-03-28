package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.domain.puzzle.rank.api.request.RankResultRequest;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankEndResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankResultResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankStartResponse;
import com.renzzle.backend.domain.puzzle.rank.domain.RankSessionData;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
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

    public RankStartResponse startRankGame(UserEntity user) {
        Long userId = user.getId();
        String redisKey = String.valueOf(userId);

        // TODO : 문제를 받은 뒤, 해당 문제를 틀렸다고 가정한 값을 미리 DB에 업데이트
        // 단, 여기 값을 수정해야함.
        user.updateRatingTo(user.getRating() - 30);
        user.updateMmrTo(user.getMmr() - 20);
        userRepository.save(user);

        //TODO: 사용자의 레에팅에 알맞는 문제를 찾는다. 사용자 레이팅에서 승률 60% 정도의 문제로 시작

        RankSessionData sessionData = new RankSessionData();

        sessionData.setUserId(userId);  // sessionId == userId
        sessionData.setBoardState("a1a2a3a4");
        sessionData.setLastProblemRating(1000);
        sessionData.setRatingBeforePenalty(1500);
        sessionData.setMmrBeforePenalty(1500);
        sessionData.setInitialMmr(user.getMmr());
        sessionData.setWinnerColor("BLACK");

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

        if (request.isSolved()) {
            // TODO: ELO를 통한 레이팅 값 업데이트
            double updatedRating = session.getRatingBeforePenalty() + 30;
            session.setRatingBeforePenalty(updatedRating);
            double updatedMmr = session.getRatingBeforePenalty() + 20;
            session.setRatingBeforePenalty(updatedMmr);
        }

        // TODO: 이전 문제 저장. 이 때 문제의 풀이 여부도 저장한다.
        /*
        사용자가 랭킹전에서 마주한 문제는 어디에 저장해둬야하는거지?
         */
//        trainingPuzzleRepository.saveProblemResult(session);

        // TODO : 문제를 풀고 난 뒤, 해당 문제를 틀렸다고 가정한 값을 미리 DB에 업데이트
        // 단, 여기 값을 수정해야함.
        user.updateRatingTo(user.getRating() - 30);
        user.updateMmrTo(user.getMmr() - 20);
        userRepository.save(user);

        // TODO: 새 문제를 ELO 시스템에 의해 할당 및 세션 갱신
        session.setBoardState("a1a3a5f3g5");
        session.setLastProblemRating(950);
        session.setWinnerColor("WHITE");
        session.setRatingBeforePenalty(session.getLastProblemRating());

        redisTemplate.opsForValue().set(redisKey, session, currentTTL, TimeUnit.MINUTES);

        return RankResultResponse.builder()
                .boardStatus(session.getBoardState())
                .winColor(session.getWinnerColor())
                .build();
    }

    public RankEndResponse endRankGame(UserEntity user) {
        String redisKey = String.valueOf( user.getId());
        RankSessionData session = redisTemplate.opsForValue().get(redisKey);
        if (session != null) {
            redisTemplate.delete(redisKey);
//            trainingPuzzleRepository.saveProblemResult(session);
        }
        return RankEndResponse.builder()
                .rating(user.getRating())
                .build();
    }

    private double calculateAdjustedRating(double currentRating, double puzzleRating, boolean isSolved) {
        final int K = 30;
        double expectedScore = 1.0 / (1.0 + Math.pow(10, (puzzleRating - currentRating) / 400));
        double actualScore = isSolved ? 1.0 : 0.0;
        double ratingChange = K * (actualScore - expectedScore);
        return currentRating + ratingChange;
    }
}
