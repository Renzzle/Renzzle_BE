package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.domain.appinfo.service.AppInfoService;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.projection.AuthorStatsProjection;
import com.renzzle.backend.domain.puzzle.community.dao.projection.SolvedCountProjection;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.rank.api.request.RankResultRequest;
import com.renzzle.backend.domain.puzzle.rank.api.response.*;
import com.renzzle.backend.domain.puzzle.rank.dao.LatestRankPuzzleRepository;
import com.renzzle.backend.domain.puzzle.rank.domain.LatestRankPuzzle;
import com.renzzle.backend.domain.puzzle.rank.domain.RankSessionData;
import com.renzzle.backend.domain.puzzle.rank.service.dto.NextPuzzleResult;
import com.renzzle.backend.domain.puzzle.shared.util.ELOUtils;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.dao.projection.UserNicknameProjection;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.renzzle.backend.domain.puzzle.shared.util.ELOUtils.TARGET_WIN_PROBABILITY;
import static com.renzzle.backend.domain.puzzle.shared.util.RatingUtil.MAX_RATING;
import static com.renzzle.backend.domain.puzzle.shared.util.RatingUtil.MIN_RATING;
import static com.renzzle.backend.global.common.constant.ItemPrice.RANK_REWARD;
import static com.renzzle.backend.global.common.constant.StringConstant.DELETED_USER;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankService {

    // Sorted sets of user ids scored by rating or puzzler score
    private static final String RATING_RANKING_KEY = "ranking:rating";
    private static final String PUZZLER_RANKING_KEY = "ranking:puzzler";
    private static final String RANKING_TEMP_KEY_SUFFIX = ":tmp";
    private static final int RANKING_SIZE = 100;

    private final RedisTemplate<String, RankSessionData> redisTemplate;
    private final TrainingPuzzleRepository trainingPuzzleRepository;
    private final CommunityPuzzleRepository communityPuzzleRepository;
    private final UserRepository userRepository;
    private final LatestRankPuzzleRepository latestRankPuzzleRepository;
    private final UserCommunityPuzzleRepository userCommunityPuzzleRepository;
    private final Clock clock;
    private final StringRedisTemplate redisRankingTemplate;
    private final AppInfoService appInfoService;

    @Value("${rank.session.ttl}")
    private long sessionTTLSeconds;

    @Transactional
    public RankStartResponse startRankGame(UserEntity userData) {
        // Row lock so concurrent start/result calls can't interleave
        UserEntity user = userRepository.findByIdForUpdate(userData.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));
        Long userId = user.getId();
        String redisKey = String.valueOf(userId);

        List<LatestRankPuzzle> existingPuzzles = latestRankPuzzleRepository.findAllByUser(user);
        if (!existingPuzzles.isEmpty()) {
            latestRankPuzzleRepository.deleteAll(existingPuzzles);
        }

        double originalMmr = user.getMmr();
        double originalRating = user.getRating();

        NextPuzzleResult nextPuzzle = getNextPuzzle(originalMmr, TARGET_WIN_PROBABILITY, user);

        double mmrPenalty = ELOUtils.calculateMMRDecrease(originalMmr, nextPuzzle.rating());
        double ratingPenalty = ELOUtils.calculateRatingDecrease(originalRating, nextPuzzle.rating());

        user.updateMmrTo(originalMmr + mmrPenalty);
        user.updateRatingTo(originalRating + ratingPenalty);
        userRepository.save(user);

        latestRankPuzzleRepository.save(
                assignPuzzle(user, nextPuzzle, originalRating, originalMmr, TARGET_WIN_PROBABILITY));

        RankSessionData sessionData = new RankSessionData();
        sessionData.setUserId(userId);
        sessionData.setBoardState(nextPuzzle.boardStatus());
        sessionData.setWinnerColor(nextPuzzle.winColor().getName());
        sessionData.setStarted(true);

        writeSessionAfterCommit(redisKey, sessionData, sessionTTLSeconds);

        return RankStartResponse.builder()
                .boardStatus(nextPuzzle.boardStatus())
                .winColor(nextPuzzle.winColor().getName())
                .build();
    }

    @Transactional
    public RankResultResponse resultRankGame(UserEntity userData, RankResultRequest request) {

        UserEntity user = userRepository.findByIdForUpdate(userData.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));

        String redisKey = String.valueOf(user.getId());
        RankSessionData session = getSessionOrThrow(redisKey);

        if (!session.isStarted()) {
            throw new CustomException(ErrorCode.EMPTY_SESSION_DATA);
        }

        long currentTTL = getRemainingTtlOrThrow(redisKey);

        LatestRankPuzzle previousPuzzle = latestRankPuzzleRepository
                .findTopByUserOrderByIdDesc(user)
                .orElseThrow(() -> new CustomException(ErrorCode.LATEST_PUZZLE_NOT_FOUND));

        // Another board means the app is resending a result whose response it never got
        if (request.boardStatus() != null && !request.boardStatus().equals(previousPuzzle.getBoardStatus())) {
            return replayLostResponse(user, request.boardStatus(), previousPuzzle);
        }

        previousPuzzle.solvedUpdate(request.isSolved());

        // Revert from the assignment snapshot, not the Redis session
        double userBeforeMmr = previousPuzzle.getMmrBeforePenalty();
        double userBeforeRating = previousPuzzle.getRatingBeforePenalty();
        double lastProblemRating = previousPuzzle.getPuzzleRating();
        double winProbability = ELOUtils.nextTargetWinProbability(
                previousPuzzle.getTargetWinProbability(), request.isSolved());
        if (request.isSolved()) {
            double mmrIncrease = ELOUtils.calculateMMRIncrease(userBeforeMmr, lastProblemRating);
            double ratingIncrease = ELOUtils.calculateRatingIncrease(userBeforeRating, lastProblemRating);

            user.updateMmrTo(userBeforeMmr + mmrIncrease);
            user.updateRatingTo(userBeforeRating + ratingIncrease);

            userBeforeMmr = userBeforeMmr + mmrIncrease;
            userBeforeRating = userBeforeRating + ratingIncrease;
        } else {
            double mmrDecrease = ELOUtils.calculateMMRDecrease(userBeforeMmr, lastProblemRating);
            double ratingDecrease = ELOUtils.calculateRatingDecrease(userBeforeRating, lastProblemRating);

            user.updateMmrTo(userBeforeMmr + mmrDecrease);
            user.updateRatingTo(userBeforeRating + ratingDecrease);

            userBeforeMmr = userBeforeMmr + mmrDecrease;
            userBeforeRating = userBeforeRating + ratingDecrease;
        }

        applyResultToPuzzle(previousPuzzle, request.isSolved());

        NextPuzzleResult nextPuzzle = getNextPuzzle(userBeforeMmr, winProbability, user);

        double ratingPenalty = ELOUtils.calculateRatingDecrease(userBeforeRating, nextPuzzle.rating());
        double mmrPenalty = ELOUtils.calculateMMRDecrease(userBeforeMmr, nextPuzzle.rating());

        user.updateMmrTo(userBeforeMmr + mmrPenalty);
        user.updateRatingTo(userBeforeRating + ratingPenalty);

        userRepository.save(user);

        latestRankPuzzleRepository.save(
                assignPuzzle(user, nextPuzzle, userBeforeRating, userBeforeMmr, winProbability));

        session.setBoardState(nextPuzzle.boardStatus());
        session.setWinnerColor(nextPuzzle.winColor().getName());

        replaceSessionAfterCommit(redisKey, session, currentTTL);

        // The server can't check isSolved, so keep a trail for spotting scripted results
        log.info("Rank result. puzzleType={}, puzzleId={}, solved={}, elapsedMs={}",
                previousPuzzle.getPuzzleType(), previousPuzzle.getPuzzleId(), request.isSolved(),
                Duration.between(previousPuzzle.getAssignedAt(), clock.instant()).toMillis());

        return RankResultResponse.builder()
                .boardStatus(nextPuzzle.boardStatus())
                .winColor(nextPuzzle.winColor().getName())
                .build();
    }

    // The answer already counted, so hand back the puzzle the lost response carried instead of applying it again
    private RankResultResponse replayLostResponse(UserEntity user, String boardStatus, LatestRankPuzzle current) {
        LatestRankPuzzle answered = latestRankPuzzleRepository
                .findTopByUserAndIdLessThanOrderByIdDesc(user, current.getId())
                .filter(puzzle -> boardStatus.equals(puzzle.getBoardStatus()))
                .orElseThrow(() -> new CustomException(ErrorCode.RANK_PUZZLE_MISMATCH));

        log.info("Rank result resent after a lost response. puzzleType={}, puzzleId={}",
                answered.getPuzzleType(), answered.getPuzzleId());

        return RankResultResponse.builder()
                .boardStatus(current.getBoardStatus())
                .winColor(current.getWinColor().getName())
                .build();
    }

    private LatestRankPuzzle assignPuzzle(
            UserEntity user,
            NextPuzzleResult puzzle,
            double ratingBeforePenalty,
            double mmrBeforePenalty,
            double targetWinProbability
    ) {
        return LatestRankPuzzle.builder()
                .user(user)
                .puzzleType(puzzle.puzzleType())
                .puzzleId(puzzle.puzzleId())
                .boardStatus(puzzle.boardStatus())
                .answer(puzzle.answer())
                .winColor(puzzle.winColor())
                .isSolved(false)
                .assignedAt(clock.instant())
                .puzzleRating(puzzle.rating())
                .ratingBeforePenalty(ratingBeforePenalty)
                .mmrBeforePenalty(mmrBeforePenalty)
                .targetWinProbability(targetWinProbability)
                .build();
    }

    private void applyResultToPuzzle(LatestRankPuzzle answered, boolean solved) {
        PuzzleType type = answered.getPuzzleType();
        Long puzzleId = answered.getPuzzleId();
        if (type == null || puzzleId == null) {
            return;
        }

        Optional<Integer> rankAttemptCount = switch (type) {
            case TRAINING -> trainingPuzzleRepository.findRankAttemptCountById(puzzleId);
            case COMMUNITY -> communityPuzzleRepository.findRankAttemptCountById(puzzleId);
        };
        // Deleted since it was handed out
        if (rankAttemptCount.isEmpty()) {
            return;
        }

        double delta = ELOUtils.calculatePuzzleRatingChange(
                type, answered.getMmrBeforePenalty(), answered.getPuzzleRating(), rankAttemptCount.get(), solved);

        switch (type) {
            case TRAINING -> trainingPuzzleRepository.applyRankResult(puzzleId, delta, MIN_RATING, MAX_RATING);
            case COMMUNITY -> communityPuzzleRepository.applyRankResult(puzzleId, delta, MIN_RATING, MAX_RATING);
        }
    }

    private void writeSessionAfterCommit(String redisKey, RankSessionData session, long ttlSeconds) {
        runAfterCommit(() -> redisTemplate.opsForValue().set(redisKey, session, ttlSeconds, TimeUnit.SECONDS));
    }

    // Overwrite only, so an ended session isn't revived
    private void replaceSessionAfterCommit(String redisKey, RankSessionData session, long ttlSeconds) {
        runAfterCommit(() -> redisTemplate.opsForValue().setIfPresent(redisKey, session, ttlSeconds, TimeUnit.SECONDS));
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void runAfterRollback(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    action.run();
                }
            }
        });
    }

    private RankSessionData getSessionOrThrow(String redisKey) {
        RankSessionData session = redisTemplate.opsForValue().get(redisKey);
        if (session == null) {
            throw new CustomException(ErrorCode.EMPTY_SESSION_DATA);
        }
        return session;
    }

    private long getRemainingTtlOrThrow(String redisKey) {
        Long ttl = (redisTemplate).getExpire(redisKey, TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) {
            throw new CustomException(ErrorCode.INVALID_SESSION_TTL);
        }
        return ttl;
    }

    @Transactional
    public RankEndResponse endRankGame(UserEntity userData) {
        // Lock first so a repeated end finds the session already claimed
        UserEntity user = userRepository.findByIdForUpdate(userData.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));

        String redisKey = String.valueOf(user.getId());
        RankSessionData session = getSessionOrThrow(redisKey);
        if (!session.isStarted()) {
            throw new CustomException(ErrorCode.IS_NOT_STARTED);
        }

        Long remainingTtl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        long restoreTtl = (remainingTtl != null && remainingTtl > 0) ? remainingTtl : sessionTTLSeconds;
        if (!Boolean.TRUE.equals(redisTemplate.delete(redisKey))) {
            throw new CustomException(ErrorCode.EMPTY_SESSION_DATA);
        }
        // Restore on rollback so the game can still be ended
        runAfterRollback(() -> redisTemplate.opsForValue().set(redisKey, session, restoreTtl, TimeUnit.SECONDS));

        List<LatestRankPuzzle> puzzles = latestRankPuzzleRepository.findAllByUser(user);
        int solvedCount = (int) puzzles.stream()
                .filter(LatestRankPuzzle::getIsSolved)
                .count();
        int reward = solvedCount * appInfoService.getPrice(RANK_REWARD);

        user.getReward(reward);

        log.info("Rank game ended. puzzles={}, solved={}, reward={}, rating={}",
                puzzles.size(), solvedCount, reward, user.getRating());

        return RankEndResponse.builder()
                .rating(user.getRating())
                .reward(reward)
                .build();
    }

    NextPuzzleResult getNextPuzzle(double originalMmr, double targetWinProbability, UserEntity user) {
        // Take the closest candidates from each source, then pick one at random
        double desiredRating = ELOUtils.getProblemRatingForTargetWinProbability(originalMmr, targetWinProbability);
        int windowSize = 5;

        List<TrainingPuzzle> trainingPuzzles =
                trainingPuzzleRepository.findAvailableTrainingPuzzlesSortedByRating(user);
        List<CommunityPuzzle> communityPuzzles =
                communityPuzzleRepository.findAvailableCommunityPuzzlesSortedByRating(user);

        List<TrainingPuzzle> selectedTrainings = pickNearByWindow(trainingPuzzles, desiredRating, windowSize);
        List<CommunityPuzzle> selectedCommunities = pickNearByWindow(communityPuzzles, desiredRating, windowSize);

        List<Object> allCandidates = new ArrayList<>();
        allCandidates.addAll(selectedTrainings);
        allCandidates.addAll(selectedCommunities);

        if (allCandidates.isEmpty()) {
            throw new CustomException(ErrorCode.CANNOT_FIND_RANK_PUZZLE);
        }

        Collections.shuffle(allCandidates);

        Object selected = allCandidates.get(0);

        if (selected instanceof TrainingPuzzle puzzle) {
            return new NextPuzzleResult(PuzzleType.TRAINING, puzzle.getId(),
                    puzzle.getBoardStatus(), puzzle.getAnswer(), puzzle.getWinColor(), puzzle.getRating());
        }

        if (selected instanceof CommunityPuzzle puzzle) {
            return new NextPuzzleResult(PuzzleType.COMMUNITY, puzzle.getId(),
                    puzzle.getBoardStatus(), puzzle.getAnswer(), puzzle.getWinColor(), puzzle.getRating());
        }
        throw new CustomException(ErrorCode.INVALID_RANK_PUZZLE_TYPE);
    }

    private <T> List<T> pickNearByWindow(List<T> sorted, double targetRating, int windowSize) {
        // Closest windowSize within 200, falling back to the closest overall
        if (sorted.isEmpty()) return Collections.emptyList();

        double maxDiff = 200.0;

        List<T> within200 = new ArrayList<>();
        for (T item : sorted) {
            double rating = getRating(item);
            if (Math.abs(rating - targetRating) < maxDiff) {
                within200.add(item);
            }
        }
        within200.sort(Comparator.comparingDouble(o -> Math.abs(getRating(o) - targetRating)));
        if (within200.size() >= windowSize) {
            return within200.subList(0, windowSize);
        }

        List<T> sortedByClosest = new ArrayList<>(sorted);
        sortedByClosest.sort(Comparator.comparingDouble(o -> Math.abs(getRating(o) - targetRating)));
        int limit = Math.min(windowSize, sortedByClosest.size());
        return sortedByClosest.subList(0, limit);
    }

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

        // Mid-game the newest puzzle is the one on the board, so its answer waits until the game ends
        RankSessionData session = redisTemplate.opsForValue().get(String.valueOf(user.getId()));
        if (session != null && session.isStarted()) {
            long currentPuzzleId = puzzles.stream().mapToLong(LatestRankPuzzle::getId).max().orElse(-1);
            puzzles = puzzles.stream().filter(puzzle -> puzzle.getId() != currentPuzzleId).toList();
        }

        return puzzles.stream()
                .map(puzzle -> RankArchive.builder()
                        .boardStatus(puzzle.getBoardStatus())
                        .answer(puzzle.getAnswer())
                        .isSolved(puzzle.getIsSolved())
                        .winColor(puzzle.getWinColor().getName())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public GetRatingRankingResponse getRatingRanking(UserEntity userData) {
        List<UserRatingRankInfo> top100 = extractTopRankedUsers(
                RATING_RANKING_KEY,
                (rank, nickname, rating) -> UserRatingRankInfo.builder()
                        .rank(rank)
                        .nickname(nickname)
                        .rating(rating)
                        .build()
        );

        UserRatingRankInfo myInfo = UserRatingRankInfo.builder()
                .rank(findMyRank(RATING_RANKING_KEY, userData).rank())
                .nickname(userData.getNickname())
                .rating(userData.getRating())
                .build();

        return GetRatingRankingResponse.builder()
                .top100(top100)
                .myRatingRank(myInfo)
                .build();
    }

    @Transactional(readOnly = true)
    public GetMyRatingResponse getMyRating(UserEntity userData) {
        UserEntity user = userRepository.findById(userData.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));

        return GetMyRatingResponse.builder()
                .rating(user.getRating())
                .build();
    }

    @Transactional(readOnly = true)
    public GetPuzzlerRankingResponse getPuzzlerRanking(UserEntity user) {
        List<UserPuzzlerRankInfo> top100 = extractTopRankedUsers(
                PUZZLER_RANKING_KEY,
                (rank, nickname, score) -> UserPuzzlerRankInfo.builder()
                        .rank(rank)
                        .nickname(nickname)
                        .score(score)
                        .build()
        );

        // Taken from the full ranking, since the user may be outside the top 100
        MyRank myRank = findMyRank(PUZZLER_RANKING_KEY, user);

        UserPuzzlerRankInfo myInfo = UserPuzzlerRankInfo.builder()
                .rank(myRank.rank())
                .nickname(user.getNickname())
                .score(myRank.score())
                .build();

        return GetPuzzlerRankingResponse.builder()
                .top100(top100)
                .myPuzzlerRank(myInfo)
                .build();
    }


    @FunctionalInterface
    private interface RankEntryBuilder<R> {
        R build(int rank, String nickname, double score);
    }

    // Nicknames are read now rather than stored in the ranking, so a renamed user shows the new one
    private <R> List<R> extractTopRankedUsers(String key, RankEntryBuilder<R> builder) {
        Set<ZSetOperations.TypedTuple<String>> entries =
                Optional.ofNullable(redisRankingTemplate.opsForZSet()
                                .reverseRangeWithScores(key, 0, RANKING_SIZE - 1))
                        .orElse(Collections.emptySet());
        if (entries.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = entries.stream()
                .map(entry -> Long.valueOf(entry.getValue()))
                .toList();
        Map<String, String> nicknames = userRepository.findNicknamesByIdIn(userIds).stream()
                .collect(Collectors.toMap(user -> String.valueOf(user.getId()), UserNicknameProjection::getNickname));

        List<R> result = new ArrayList<>();
        int currentRank = 1;
        double lastScore = -1;
        int rankCounter = 0;

        for (ZSetOperations.TypedTuple<String> entry : entries) {
            double score = entry.getScore();
            rankCounter++;

            if (Double.compare(score, lastScore) != 0) {
                currentRank = rankCounter;
                lastScore = score;
            }

            // Missing when the user withdrew after the ranking was built
            String nickname = nicknames.getOrDefault(entry.getValue(), DELETED_USER);
            result.add(builder.build(currentRank, nickname, score));
        }

        return result;
    }

    // Rank -1 and score 0 when the user isn't in the ranking
    private record MyRank(int rank, double score) {}

    // Tied users share a rank, so it is one more than the number of higher scores
    private MyRank findMyRank(String key, UserEntity user) {
        ZSetOperations<String, String> ranking = redisRankingTemplate.opsForZSet();
        Double score = ranking.score(key, String.valueOf(user.getId()));
        if (score == null) {
            return new MyRank(-1, 0.0);
        }

        // Counting from the next double up leaves out the user's own score
        Long higher = ranking.count(key, Math.nextUp(score), Double.POSITIVE_INFINITY);
        return new MyRank(Objects.requireNonNullElse(higher, 0L).intValue() + 1, score);
    }

    // Withdrawn users leave the rankings now rather than at the next rebuild, once the withdrawal commits
    public void removeFromRankings(Long userId) {
        String member = String.valueOf(userId);
        runAfterCommit(() -> {
            redisRankingTemplate.opsForZSet().remove(RATING_RANKING_KEY, member);
            redisRankingTemplate.opsForZSet().remove(PUZZLER_RANKING_KEY, member);
        });
    }

    @Scheduled(fixedDelay = 1000 * 60 * 5) // Runs at startup, then 5 minutes after each run ends
    public void updateRankingCache() {
        Instant oneMonthAgo = Instant.now(clock).minus(30, ChronoUnit.DAYS);

        Map<String, Double> ratings = new HashMap<>();
        for (UserEntity user : latestRankPuzzleRepository.findActiveUsersWithinPeriod(oneMonthAgo)) {
            ratings.put(String.valueOf(user.getId()), user.getRating());
        }
        publishRanking(RATING_RANKING_KEY, ratings);

        List<UserEntity> creators = communityPuzzleRepository.findUsersWhoCreatedPuzzlesSince(oneMonthAgo);
        List<UserEntity> solvers = userCommunityPuzzleRepository.findUsersWhoSolvedPuzzlesSince(oneMonthAgo);

        Set<Long> activePuzzlerIds = new HashSet<>();
        creators.forEach(user -> activePuzzlerIds.add(user.getId()));
        solvers.forEach(user -> activePuzzlerIds.add(user.getId()));

        publishRanking(PUZZLER_RANKING_KEY, puzzlerScores(activePuzzlerIds));
    }

    // Two grouped queries cover every user, instead of four queries per user
    private Map<String, Double> puzzlerScores(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> solvedCounts = userCommunityPuzzleRepository.countSolvedByUserIds(userIds).stream()
                .collect(Collectors.toMap(SolvedCountProjection::getUserId, SolvedCountProjection::getSolvedCount));
        Map<Long, AuthorStatsProjection> authorStats = communityPuzzleRepository.sumAuthorStatsByUserIds(userIds).stream()
                .collect(Collectors.toMap(AuthorStatsProjection::getUserId, Function.identity()));

        Map<String, Double> scores = new HashMap<>();
        for (Long userId : userIds) {
            // A user missing from a result has nothing of that kind yet
            long a = solvedCounts.getOrDefault(userId, 0L);
            AuthorStatsProjection stats = authorStats.get(userId);
            long b = stats != null ? stats.getPuzzleCount() : 0;
            long c = stats != null ? Math.max(0, stats.getLikeSum() - stats.getDislikeSum()) : 0;

            double score = Math.log((a + 1.0) * Math.pow(b + 1.0, 2) * Math.pow(c + 1.0, 3) + 1) * 100;
            scores.put(String.valueOf(userId), Math.floor(score));
        }
        return scores;
    }

    // Built under a temporary key and renamed over the live one, so readers never see a half-filled ranking
    private void publishRanking(String key, Map<String, Double> scores) {
        if (scores.isEmpty()) {
            // ZADD needs at least one member, so an empty ranking is a deleted key
            redisRankingTemplate.delete(key);
            return;
        }

        Set<ZSetOperations.TypedTuple<String>> entries = scores.entrySet().stream()
                .map(entry -> ZSetOperations.TypedTuple.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());

        String tempKey = key + RANKING_TEMP_KEY_SUFFIX;
        redisRankingTemplate.delete(tempKey);
        redisRankingTemplate.opsForZSet().add(tempKey, entries);
        redisRankingTemplate.rename(tempKey, key);
    }
}
