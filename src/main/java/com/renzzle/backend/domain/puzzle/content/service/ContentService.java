package com.renzzle.backend.domain.puzzle.content.service;

import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzlesResponse;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.content.api.request.GetRecommendRequest;
import com.renzzle.backend.domain.puzzle.content.api.response.GetTrendPuzzlesResponse;
import com.renzzle.backend.domain.puzzle.content.api.response.getRecommendPackResponse;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {
    private final SolvedTrainingPuzzleRepository solvedTrainingPuzzleRepository;
    private final CommunityPuzzleRepository communityPuzzleRepository;
    private final PackTranslationRepository packTranslationRepository;
    private final UserPackRepository userPackRepository;
    private final PackRepository packRepository;
    private final UserCommunityPuzzleRepository userCommunityPuzzleRepository;
    private final Clock clock;
    public getRecommendPackResponse getRecommendedPack(GetRecommendRequest request, UserEntity user) {
        Long userId = user.getId();

        // 가장 최근에 푼 퍼즐 하나 조회
        Optional<SolvedTrainingPuzzle> recentSolvedOpt = solvedTrainingPuzzleRepository
                .findTopByUserOrderBySolvedAtDesc(userId);

        if (recentSolvedOpt.isEmpty()) {
            // 퍼즐을 푼 적이 없는 신규 사용자 -> 기본 추천 제공
            return createDefaultRecommendedPack(request);
        }

        // 퍼즐을 푼 기록이 있는 경우
        SolvedTrainingPuzzle recentSolved = recentSolvedOpt.get();

        Pack pack = recentSolved.getPuzzle().getPack();
        if (pack == null) {
            throw new CustomException(ErrorCode.NO_SUCH_TRAINING_PACK);
        }

        // 번역 정보 조회
        PackTranslation translation = packTranslationRepository
                .findByPackAndLangCode(pack, LangCode.getLangCode(request.langCode()))
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_PACK_TRANSLATION));

        // 유저의 pack 진행 정보 조회
        UserPack userPack = userPackRepository
                .findByUserIdAndPackId(userId, pack.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NO_USER_PROGRESS_FOR_PACK));

//        boolean locked = (userPack == null);
        int solvedCount = (userPack != null) ? userPack.getSolved_count() : 0;

        return getRecommendPackResponse.builder()
                .id(pack.getId())
                .title(translation.getTitle())
                .author(translation.getAuthor())
                .description(translation.getDescription())
                .price(pack.getPrice())
                .totalPuzzleCount(pack.getPuzzleCount())
                .solvedPuzzleCount(solvedCount)
                .locked(false)  //  userPack 은 항상 not null
                .build();
    }

    private getRecommendPackResponse createDefaultRecommendedPack(GetRecommendRequest request) {
        // 1. 가장 id가 낮은 Pack 조회
        Pack pack = packRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_TRAINING_PACK));

        // 2. 번역 정보 조회
        PackTranslation translation = packTranslationRepository
                .findByPackAndLangCode(pack, LangCode.getLangCode(request.langCode()))
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_PACK_TRANSLATION));

        // 3. 결과 반환
        return getRecommendPackResponse.builder()
                .id(pack.getId())
                .title(translation.getTitle())
                .author(translation.getAuthor())
                .description(translation.getDescription())
                .price(pack.getPrice())
                .totalPuzzleCount(pack.getPuzzleCount())
                .solvedPuzzleCount(0) // 아직 아무 문제도 풀지 않았으니까
                .locked(false)        // 기본 추천이므로 잠금 아님
                .build();
    }

    public GetTrendPuzzlesResponse getTrendCommunityPuzzles(UserEntity user) {
        Set<Long> selectedIds = new HashSet<>();
        List<GetCommunityPuzzlesResponse> result = new ArrayList<>();

        Instant instant = clock.instant();

        Instant oneWeekAgo = instant.minus(7, ChronoUnit.DAYS);

        // 최근 1주일 내 퍼즐 조회
        List<CommunityPuzzle> puzzlesIn7Days = communityPuzzleRepository
                .findByCreatedAtAfter(oneWeekAgo);

        // 정렬 기준 적용 후 선정
        List<CommunityPuzzle> sortedRecent = puzzlesIn7Days.stream()
                .sorted(trendComparator())
                .toList();

        selectTrendPuzzles(sortedRecent, result, selectedIds, user);

        //부족 시 → 최근 1주일 이전 퍼즐 중 최신 30개
        if (result.size() < 5) {
            List<CommunityPuzzle> latest30 = communityPuzzleRepository
                    .findTop30ByCreatedAtBeforeOrderByCreatedAtDesc(oneWeekAgo);

            List<CommunityPuzzle> sortedBackup = latest30.stream()
                    .filter(p -> !selectedIds.contains(p.getId()))
                    .sorted(trendComparator())
                    .toList();

            selectTrendPuzzles(sortedBackup, result, selectedIds, user);
        }

        return new GetTrendPuzzlesResponse(result);
    }

    private void selectTrendPuzzles(
            List<CommunityPuzzle> puzzles,
            List<GetCommunityPuzzlesResponse> result,
            Set<Long> selectedIds,
            UserEntity user
    ) {
        for (CommunityPuzzle p : puzzles) {
            if (result.size() >= 5) break;

            boolean added = selectedIds.add(p.getId());
            if (!added) {
                throw new CustomException(ErrorCode.TREND_PUZZLE_DUPLICATED);
            }

            result.add(convertToResponse(p, user));
        }
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

    private Comparator<CommunityPuzzle> trendComparator() {
        return Comparator
                .comparingInt((CommunityPuzzle p) -> p.getLikeCount() - p.getDislikeCount()).reversed()
                .thenComparingInt(CommunityPuzzle::getView).reversed()
                .thenComparingLong(CommunityPuzzle::getId);
    }
}
