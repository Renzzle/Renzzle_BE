package com.renzzle.backend.domain.puzzle.training.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RankService {

//    private final RedisTemplate<String, Object> redisTemplate;
//    private final TrainingPuzzleRepository trainingPuzzleRepository;
//
//    @Transactional
//    public RankStartResponse RankStart(UserEntity user) {
//
//        double userRating = user.getMmr();
//        double minRating = userRating - 100;
//        double maxRating = userRating + 100;
//
//        TrainingPuzzle puzzle = trainingPuzzleRepository
//                .findRandomByRatingBetween(minRating, maxRating)
//                .orElseThrow(() -> new CustomException(ErrorCode.GLOBAL_NOT_FOUND));
//
//        String boardStatus = puzzle.getBoardStatus();
//        WinColor winColor = puzzle.getWinColor();
//
//        double adjustedRating = calculateAdjustedRating(user.getMmr(), puzzle.getRating(),false);
//
//        // 세션 ID 생성
//        Long sessionId = generateSessionId();
//
//        // Redis에 세션 정보 저장
//        String key = "ranking:session:" + sessionId;
//        Map<String, Object> sessionData = new HashMap<>();
//        sessionData.put("userId", user.getId());
//        sessionData.put("boardStatus", boardStatus);
//        sessionData.put("winColor", winColor);
//        sessionData.put("adjustRating", adjustedRating);
//
//        redisTemplate.opsForHash().putAll(key, sessionData);
//
//        // TTL 5분 설정 (300초)
//        redisTemplate.expire(key, 5, TimeUnit.MINUTES);
//
//        return RankStartResponse.builder()
//                .sessionId(sessionId)
//                .boardStatus(boardStatus)
//                .winColor(winColor)
//                .build();
//    }
//
//
//
//    private Long generateSessionId() {
//        Long sessionId = redisTemplate.opsForValue().increment("RANK_SESSION_SEQ");
//        if (sessionId == null) {
//            throw new CustomException(ErrorCode.SESSION_GENERATION_FAILED);
//        }
//        return sessionId;
//    }
//
//    private double calculateAdjustedRating(double currentRating, double puzzleRating, boolean isSolved) {
//        final int K = 30;
//        double expectedScore = 1.0 / (1.0 + Math.pow(10, (puzzleRating - currentRating) / 400));
//        double actualScore = isSolved ? 1.0 : 0.0;
//        double ratingChange = K * (actualScore - expectedScore);
//        return currentRating + ratingChange;
//    }
}
