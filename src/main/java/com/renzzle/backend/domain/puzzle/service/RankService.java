package com.renzzle.backend.domain.puzzle.service;

import com.renzzle.backend.domain.puzzle.api.response.RankStartResponse;
import com.renzzle.backend.domain.puzzle.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.domain.TrainingPuzzle;
import com.renzzle.backend.domain.puzzle.domain.WinColor;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.domain.user.service.UserService;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RankService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final TrainingPuzzleRepository trainingPuzzleRepository;

    @Transactional
    public RankStartResponse RankStart(UserEntity user) {

        double userRating = user.getLatentRating();
        double minRating = userRating - 100;
        double maxRating = userRating + 100;

        TrainingPuzzle puzzle = trainingPuzzleRepository
                .findRandomByRatingBetween(minRating, maxRating)
                .orElseThrow(() -> new CustomException(ErrorCode.GLOBAL_NOT_FOUND));

        String boardStatus = puzzle.getBoardStatus();
        WinColor winColor = puzzle.getWinColor();

        // 세션 ID 생성
        Long sessionId = generateSessionId();

        // 3-3. Redis에 세션 정보 저장
        String key = "ranking:session:" + sessionId;
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("userId", user.getId());
        sessionData.put("boardStatus", boardStatus);
        sessionData.put("winColor", winColor);
        // 기타 필요한 데이터 (예: solvedCount, 점수, 시작 시간 등)
        redisTemplate.opsForHash().putAll(key, sessionData);

        // 3-4. TTL 5분 설정 (300초)
        redisTemplate.expire(key, 5, TimeUnit.MINUTES);

        RankStartResponse response = RankStartResponse.builder()
                .sessionId(sessionId)
                .boardStatus(boardStatus)
                .winColor(winColor)
                .build();

        return response;
    }

    private Long generateSessionId() {
        Long sessionId = redisTemplate.opsForValue().increment("RANK_SESSION_SEQ");
        if (sessionId == null) {
            throw new CustomException(ErrorCode.SESSION_GENERATION_FAILED);
        }
        return sessionId;
    }


}
