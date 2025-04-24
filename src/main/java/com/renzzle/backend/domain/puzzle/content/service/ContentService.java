package com.renzzle.backend.domain.puzzle.content.service;

import com.renzzle.backend.domain.puzzle.content.api.request.GetRecommendRequest;
import com.renzzle.backend.domain.puzzle.content.api.response.getRecommendPackResponse;
import com.renzzle.backend.domain.puzzle.training.dao.PackTranslationRepository;
import com.renzzle.backend.domain.puzzle.training.dao.SolvedTrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.dao.UserPackRepository;
import com.renzzle.backend.domain.puzzle.training.domain.Pack;
import com.renzzle.backend.domain.puzzle.training.domain.PackTranslation;
import com.renzzle.backend.domain.puzzle.training.domain.SolvedTrainingPuzzle;
import com.renzzle.backend.domain.puzzle.training.domain.UserPack;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {
    private final SolvedTrainingPuzzleRepository solvedTrainingPuzzleRepository;
    private final PackTranslationRepository packTranslationRepository;
    private final UserPackRepository userPackRepository;
    public getRecommendPackResponse getRecommendedPack(GetRecommendRequest request, UserEntity user) {
        Long userId = user.getId();

        // 가장 최근에 푼 퍼즐 하나 조회
        SolvedTrainingPuzzle recentSolved = solvedTrainingPuzzleRepository
                .findTopByUserOrderBySolvedAtDesc(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_TRAINING_PACKS));

        Pack pack = recentSolved.getPuzzle().getPack();

        // 번역 정보 조회
        PackTranslation translation = packTranslationRepository
                .findByPack_IdAndLanguageCode(pack.getId(), request.lang().name())
                .orElse(null);

        // 유저의 pack 진행 정보 조회
        UserPack userPack = userPackRepository
                .findByUserIdAndPackId(userId, pack.getId())
                .orElse(null);

        boolean locked = (userPack == null);
        int solvedCount = (userPack != null) ? userPack.getSolved_count() : 0;

        return getRecommendPackResponse.builder()
                .id(pack.getId())
                .title(translation != null ? translation.getTitle() : null)
                .author(translation != null ? translation.getAuthor() : null)
                .description(translation != null ? translation.getDescription() : null)
                .price(pack.getPrice())
                .totalPuzzleCount(pack.getPuzzleCount())
                .solvedPuzzleCount(solvedCount)
                .locked(locked)
                .build();
    }
}
