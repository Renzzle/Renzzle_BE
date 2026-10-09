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
import com.renzzle.backend.domain.puzzle.training.domain.Pack;
import com.renzzle.backend.domain.puzzle.training.domain.PackTranslation;
import com.renzzle.backend.domain.puzzle.training.domain.SolvedTrainingPuzzle;
import com.renzzle.backend.domain.puzzle.training.domain.UserPack;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {
    private static final int TREND_SIZE = 5;
    // A like counts half after two days and a quarter after four
    private static final Duration TREND_HALF_LIFE = Duration.ofDays(2);

    private final SolvedTrainingPuzzleRepository solvedTrainingPuzzleRepository;
    private final CommunityPuzzleRepository communityPuzzleRepository;
    private final PackTranslationRepository packTranslationRepository;
    private final UserPackRepository userPackRepository;
    private final PackRepository packRepository;
    private final UserCommunityPuzzleRepository userCommunityPuzzleRepository;
    private final Clock clock;
    public GetRecommendPackResponse getRecommendedPack(GetRecommendRequest request, UserEntity user) {

        Long userId = user.getId();

        Optional<SolvedTrainingPuzzle> recentSolvedOpt = solvedTrainingPuzzleRepository
                .findTopByUserOrderBySolvedAtDesc(userId);

        if (recentSolvedOpt.isEmpty()) {
            return createDefaultRecommendedPack(request);
        }

        SolvedTrainingPuzzle recentSolved = recentSolvedOpt.get();
        Pack pack = recentSolved.getPuzzle().getPack();

        if (pack == null) {
            throw new CustomException(ErrorCode.NO_SUCH_TRAINING_PACK);
        }

        LangCode requestedLang = LangCode.getLangCode(request.langCode());
        LangCode defaultLang = LangCode.getLangCode(LangCode.LangCodeName.EN);

        PackTranslation translation = packTranslationRepository.findByPackAndLangCode(pack, requestedLang)
                .orElseGet(() ->
                        packTranslationRepository.findByPackAndLangCode(pack, defaultLang)
                                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_PACK_TRANSLATION))
                );

        UserPack userPack = userPackRepository
                .findByUserIdAndPackId(userId, pack.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NO_USER_PROGRESS_FOR_PACK));

        int solvedCount = userPack.getSolvedCount();

        return GetRecommendPackResponse.builder()
                .id(pack.getId())
                .title(translation.getTitle())
                .author(translation.getAuthor())
                .description(translation.getDescription())
                .price(pack.getPrice())
                .totalPuzzleCount(pack.getPuzzleCount())
                .solvedPuzzleCount(solvedCount)
                .locked(false)
                .build();
    }

    private GetRecommendPackResponse createDefaultRecommendedPack(GetRecommendRequest request) {
        Pack pack = packRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_TRAINING_PACK));

        LangCode requestedLang = LangCode.getLangCode(request.langCode());
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
                .solvedPuzzleCount(0)
                .locked(false)
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
