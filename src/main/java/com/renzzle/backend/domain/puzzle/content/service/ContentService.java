package com.renzzle.backend.domain.puzzle.content.service;

import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzlesResponse;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.content.api.request.GetRecommendRequest;
import com.renzzle.backend.domain.puzzle.content.api.response.GetTrendPuzzlesResponse;
import com.renzzle.backend.domain.puzzle.content.api.response.GetRecommendPackResponse;
import com.renzzle.backend.domain.puzzle.training.dao.PackRepository;
import com.renzzle.backend.domain.puzzle.training.dao.PackTranslationRepository;
import com.renzzle.backend.domain.puzzle.training.dao.SolvedTrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.dao.UserPackRepository;
import com.renzzle.backend.domain.puzzle.training.dao.projection.PackLastSolvedProjection;
import com.renzzle.backend.domain.puzzle.training.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.training.domain.Pack;
import com.renzzle.backend.domain.puzzle.training.domain.PackTranslation;
import com.renzzle.backend.domain.puzzle.training.domain.UserPack;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {
    private static final int TREND_SIZE = 5;
    // A like counts half after two days and a quarter after four
    private static final Duration TREND_HALF_LIFE = Duration.ofDays(2);
    // Easiest first; DifficultyName lists them the other way round
    private static final List<String> DIFFICULTY_ORDER = List.of(
            Difficulty.DifficultyName.LOW.name(),
            Difficulty.DifficultyName.MIDDLE.name(),
            Difficulty.DifficultyName.HIGH.name());

    private final SolvedTrainingPuzzleRepository solvedTrainingPuzzleRepository;
    private final CommunityPuzzleRepository communityPuzzleRepository;
    private final PackTranslationRepository packTranslationRepository;
    private final UserPackRepository userPackRepository;
    private final PackRepository packRepository;
    private final UserCommunityPuzzleRepository userCommunityPuzzleRepository;
    private final Clock clock;

    // The pack the user is partway through; once that one is finished, the next pack to take up
    @Transactional(readOnly = true)
    public GetRecommendPackResponse getRecommendedPack(GetRecommendRequest request, UserEntity user) {
        Long userId = user.getId();
        List<Pack> packs = packRepository.findByIsPublicTrue();
        Map<Long, UserPack> owned = userPackRepository
                .findAllByUserIdAndPackIdIn(userId, packs.stream().map(Pack::getId).toList()).stream()
                .collect(Collectors.toMap(userPack -> userPack.getPack().getId(), Function.identity()));

        Pack pack = solvedTrainingPuzzleRepository.findLatestInPublicPackByUserId(userId)
                .map(solved -> solved.getPuzzle().getPack())
                .map(latest -> isFinished(latest, owned) ? nextPack(userId, latest, packs, owned) : latest)
                // Nothing solved yet, so the user starts with the pack granted at sign-up
                .orElseGet(() -> packs.stream()
                        .filter(candidate -> Pack.STARTER_PACK_ID.equals(candidate.getId()))
                        .findFirst()
                        .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_TRAINING_PACK)));

        return toRecommendPackResponse(pack, owned.get(pack.getId()), request.langCode());
    }

    // Packs left partway come first, the furthest along leading; failing those, a pack not begun yet,
    // at the finished pack's difficulty or else the nearest one
    private Pack nextPack(Long userId, Pack finished, List<Pack> packs, Map<Long, UserPack> owned) {
        List<Pack> partway = packs.stream()
                .filter(pack -> solvedCount(pack, owned) > 0 && !isFinished(pack, owned))
                .toList();
        if (!partway.isEmpty()) {
            Map<Long, Instant> lastSolvedAt = solvedTrainingPuzzleRepository.findLastSolvedAtByPack(userId).stream()
                    .collect(Collectors.toMap(PackLastSolvedProjection::getPackId, PackLastSolvedProjection::getLastSolvedAt));
            // Equally far along, the one played last wins
            return Collections.max(partway, Comparator
                    .comparingDouble((Pack pack) -> (double) solvedCount(pack, owned) / pack.getPuzzleCount())
                    .thenComparing(pack -> lastSolvedAt.getOrDefault(pack.getId(), Instant.MIN)));
        }

        List<String> difficulties = searchOrder(finished.getDifficulty().getName());
        return packs.stream()
                .filter(pack -> pack.getPuzzleCount() > 0 && solvedCount(pack, owned) == 0)
                .min(Comparator.comparingInt((Pack pack) -> difficulties.indexOf(pack.getDifficulty().getName()))
                        // Packs the user already has before ones still to buy
                        .thenComparing(pack -> owned.containsKey(pack.getId()), Comparator.reverseOrder())
                        .thenComparing(Pack::getId))
                // Every pack is finished, so the last one played stays
                .orElse(finished);
    }

    // The same difficulty, then the harder ones, then the easier ones nearest first
    private static List<String> searchOrder(String difficulty) {
        int level = DIFFICULTY_ORDER.indexOf(difficulty);
        List<String> order = new ArrayList<>(DIFFICULTY_ORDER.subList(level, DIFFICULTY_ORDER.size()));
        List<String> easier = new ArrayList<>(DIFFICULTY_ORDER.subList(0, level));
        Collections.reverse(easier);
        order.addAll(easier);
        return order;
    }

    private static int solvedCount(Pack pack, Map<Long, UserPack> owned) {
        UserPack userPack = owned.get(pack.getId());
        return userPack != null ? userPack.getSolvedCount() : 0;
    }

    private static boolean isFinished(Pack pack, Map<Long, UserPack> owned) {
        return solvedCount(pack, owned) >= pack.getPuzzleCount();
    }

    private GetRecommendPackResponse toRecommendPackResponse(Pack pack, UserPack userPack, String langCode) {
        LangCode requestedLang = LangCode.getLangCode(langCode);
        LangCode defaultLang = LangCode.getLangCode(LangCode.LangCodeName.EN);

        PackTranslation translation = packTranslationRepository.findByPackAndLangCode(pack, requestedLang)
                .orElseGet(() ->
                        packTranslationRepository.findByPackAndLangCode(pack, defaultLang)
                                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_PACK_TRANSLATION))
                );

        return GetRecommendPackResponse.builder()
                .id(pack.getId())
                .title(translation.getTitle())
                .author(translation.getAuthor())
                .description(translation.getDescription())
                .price(pack.getPrice())
                .totalPuzzleCount(pack.getPuzzleCount())
                .solvedPuzzleCount(userPack != null ? userPack.getSolvedCount() : 0)
                // Locked until the user has the pack, as in the pack list
                .locked(userPack == null)
                .build();
    }

    // Ranked in the database, so only the puzzles shown are loaded; may return fewer than TREND_SIZE
    public GetTrendPuzzlesResponse getTrendCommunityPuzzles(UserEntity user) {
        Instant now = clock.instant();
        Instant oneWeekAgo = now.minus(7, ChronoUnit.DAYS);
        long halfLifeSeconds = TREND_HALF_LIFE.toSeconds();

        List<CommunityPuzzle> puzzles = new ArrayList<>(communityPuzzleRepository
                .findTrendPuzzlesSince(oneWeekAgo, now, halfLifeSeconds, TREND_SIZE));

        // A quiet week is filled from the latest older puzzles
        if (puzzles.size() < TREND_SIZE) {
            puzzles.addAll(communityPuzzleRepository
                    .findOlderTrendPuzzles(oneWeekAgo, now, halfLifeSeconds, TREND_SIZE - puzzles.size()));
        }

        List<GetCommunityPuzzlesResponse> result = puzzles.stream()
                .map(puzzle -> convertToResponse(puzzle, user))
                .toList();
        return new GetTrendPuzzlesResponse(result);
    }

    private GetCommunityPuzzlesResponse convertToResponse(CommunityPuzzle puzzle, UserEntity user) {

        boolean isSolved = userCommunityPuzzleRepository.checkIsSolvedPuzzle(user.getId(), puzzle.getId());

        return GetCommunityPuzzlesResponse.builder()
                .id(puzzle.getId())
                .boardStatus(puzzle.getBoardStatus())
                .authorId(puzzle.getUser().getId())
                .authorName(puzzle.getUser().getNickname())
                .depth(puzzle.getDepth())
                .winColor(puzzle.getWinColor().getName())
                .solvedCount(puzzle.getSolvedCount())
                .views(puzzle.getView())
                .likeCount(puzzle.getLikeCount())
                .createdAt(puzzle.getCreatedAt().toString())
                .isSolved(isSolved)
                .isVerified(puzzle.getIsVerified())
                .build();
    }
}
