package com.renzzle.backend.domain.puzzle.community.service;

import com.renzzle.backend.domain.puzzle.community.api.request.AddCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.api.response.AddCommunityPuzzleResponse;
import com.renzzle.backend.domain.puzzle.cache.api.request.GetCommunityPuzzlesForCacheRequest;
import com.renzzle.backend.domain.puzzle.cache.api.response.CommunityPuzzleCachePickerResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzleAnswerResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzlesResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetSingleCommunityPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.api.response.GetTrainingPuzzleForAdminResponse;
import com.renzzle.backend.domain.puzzle.community.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.projection.LikeDislikeProjection;
import com.renzzle.backend.domain.puzzle.community.domain.*;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.renzzle.backend.global.common.constant.DoubleConstant.DEFAULT_PUZZLE_RATING;
import static com.renzzle.backend.global.common.constant.ItemPrice.HINT;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final Clock clock;
    private final CommunityPuzzleRepository communityPuzzleRepository;
    private final UserCommunityPuzzleRepository userCommunityPuzzleRepository;
    private final UserRepository userRepository;

    @Transactional
    public AddCommunityPuzzleResponse addCommunityPuzzle(AddCommunityPuzzleRequest request, UserEntity user) {
        String boardKey = BoardUtils.makeBoardKey(request.boardStatus());

        CommunityPuzzle puzzle = CommunityPuzzle.builder()
                .boardStatus(request.boardStatus())
                .boardKey(boardKey)
                .answer(request.answer())
                .depth(request.depth())
                .rating(request.depth() * DEFAULT_PUZZLE_RATING) // TODO: rating formula
                .description(request.description())
                .user(user)
                .winColor(WinColor.getWinColor(request.winColor()))
                .isVerified(request.isVerified())
                .build();

        CommunityPuzzle result = communityPuzzleRepository.save(puzzle);

        return AddCommunityPuzzleResponse.builder()
                .puzzleId(result.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public List<GetCommunityPuzzlesResponse> getCommunityPuzzleList(GetCommunityPuzzleRequest request, UserEntity user) {
        List<CommunityPuzzle> puzzleList = communityPuzzleRepository.searchCommunityPuzzles(request, user.getId());

        List<GetCommunityPuzzlesResponse> response = new ArrayList<>();
        for (CommunityPuzzle puzzle : puzzleList) {
            boolean isSolved = userCommunityPuzzleRepository.checkIsSolvedPuzzle(user.getId(), puzzle.getId());

            response.add(
                    GetCommunityPuzzlesResponse.builder()
                            .id(puzzle.getId())
                            .boardStatus(puzzle.getBoardStatus())
                            .authorId(puzzle.getUser().getId())
                            .authorName(puzzle.getUser().getNickname())
                            .description(puzzle.getDescription())
                            .depth(puzzle.getDepth())
                            .winColor(puzzle.getWinColor().getName())
                            .solvedCount(puzzle.getSolvedCount())
                            .views(puzzle.getView())
                            .likeCount(puzzle.getLikeCount())
                            .createdAt(puzzle.getCreatedAt().toString())
                            .isSolved(isSolved)
                            .isVerified(puzzle.getIsVerified())
                            .build()
            );
        }

        return response;
    }

    /**
     * For admin cache entry: full board and answer. Does not increment the view count.
     */
    @Transactional(readOnly = true)
    public GetTrainingPuzzleForAdminResponse getCommunityPuzzleForAdminDetail(Long puzzleId) {
        CommunityPuzzle puzzle = communityPuzzleRepository.findById(puzzleId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));
        return GetTrainingPuzzleForAdminResponse.builder()
                .id(puzzle.getId())
                .boardStatus(puzzle.getBoardStatus())
                .answer(puzzle.getAnswer())
                .depth(puzzle.getDepth())
                .winColor(puzzle.getWinColor().getName())
                .trainingIndex(0)
                .isSolved(false)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CommunityPuzzleCachePickerResponse> getCommunityPuzzlesForCachePicker(GetCommunityPuzzlesForCacheRequest request) {
        int depthMin = request.depthMin() != null ? request.depthMin() : 1;
        int depthMax = request.depthMax() != null ? request.depthMax() : 225;
        if (depthMin > depthMax) {
            throw new CustomException("depthMin은 depthMax보다 클 수 없습니다.", ErrorCode.VALIDATION_ERROR);
        }
        int size = request.size() != null ? request.size() : 20;
        String nickname = request.authorNickname();
        if (nickname != null) {
            nickname = nickname.trim();
            if (nickname.isEmpty()) {
                nickname = null;
            }
        }
        List<CommunityPuzzle> puzzleList = communityPuzzleRepository.searchCommunityPuzzlesForCache(
                nickname,
                request.stone(),
                depthMin,
                depthMax,
                request.id(),
                size
        );
        List<CommunityPuzzleCachePickerResponse> response = new ArrayList<>();
        for (CommunityPuzzle puzzle : puzzleList) {
            response.add(
                    CommunityPuzzleCachePickerResponse.builder()
                            .id(puzzle.getId())
                            .boardStatus(puzzle.getBoardStatus())
                            .depth(puzzle.getDepth())
                            .winColor(puzzle.getWinColor().getName())
                            .description(puzzle.getDescription())
                            .authorNickname(puzzle.getUser().getNickname())
                            .build()
            );
        }
        return response;
    }

    @Transactional
    public GetSingleCommunityPuzzleResponse getCommunityPuzzleById(Long puzzleId, UserEntity user) {
        CommunityPuzzle puzzle = communityPuzzleRepository.findById(puzzleId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));

        boolean isSolved = userCommunityPuzzleRepository.checkIsSolvedPuzzle(user.getId(), puzzle.getId());

        Optional<LikeDislikeProjection> result = userCommunityPuzzleRepository
                .getMyLikeDislike(user.getId(), puzzleId);
        Boolean myLike = result.map(LikeDislikeProjection::getIsLiked).orElse(false);
        Boolean myDislike = result.map(LikeDislikeProjection::getIsDisliked).orElse(false);

        puzzle.increaseViews();

        return GetSingleCommunityPuzzleResponse.builder()
                .id(puzzle.getId())
                .boardStatus(puzzle.getBoardStatus())
                .authorId(puzzle.getUser().getId())
                .authorName(puzzle.getUser().getNickname())
                .description(puzzle.getDescription())
                .depth(puzzle.getDepth())
                .winColor(puzzle.getWinColor().getName())
                .solvedCount(puzzle.getSolvedCount())
                .views(puzzle.getView())
                .likeCount(puzzle.getLikeCount())
                .createdAt(puzzle.getCreatedAt().toString())
                .isSolved(isSolved)
                .isVerified(puzzle.getIsVerified())
                .myLike(myLike)
                .myDislike(myDislike)
                .build();
    }

    @Transactional
    public GetCommunityPuzzleAnswerResponse getCommunityPuzzleAnswer(Long puzzleId, UserEntity user) {
        UserEntity persistedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));

        CommunityPuzzle puzzle = communityPuzzleRepository.findById(puzzleId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));

        persistedUser.purchase(HINT.getPrice());

        applySolveCommunityPuzzle(puzzleId, persistedUser);

        return GetCommunityPuzzleAnswerResponse.builder()
                .answer(puzzle.getAnswer())
                .price(HINT.getPrice())
                .build();
    }

    @Transactional
    public void solveCommunityPuzzle(Long puzzleId, UserEntity user) {
        applySolveCommunityPuzzle(puzzleId, user);
    }

    private void applySolveCommunityPuzzle(Long puzzleId, UserEntity user) {
        CommunityPuzzle puzzle = communityPuzzleRepository.findById(puzzleId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));

        puzzle.increaseSolvedCount();

        int updatedRows = userCommunityPuzzleRepository.solvePuzzle(user.getId(), puzzleId, clock.instant());
        if (updatedRows == 1) {
            return;
        }

        userCommunityPuzzleRepository.save(
                UserCommunityPuzzle.builder()
                        .user(user)
                        .puzzle(puzzle)
                        .isSolved(true)
                        .solvedAt(clock.instant())
                        .build()
        );
    }

    @Transactional
    public boolean toggleLike(Long puzzleId, UserEntity user) {
        CommunityPuzzle puzzle = communityPuzzleRepository.findById(puzzleId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));

        Optional<UserCommunityPuzzle> ucp = userCommunityPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), puzzleId);
        if (ucp.isPresent()) {
            if (ucp.get().isLiked()) puzzle.decreaseLikedCount();
            else {
                if (ucp.get().isDisliked()) puzzle.decreaseDislikedCount();
                puzzle.increaseLikedCount();
            }
            return ucp.get().toggleLike(clock.instant());
        }

        userCommunityPuzzleRepository.save(
                UserCommunityPuzzle.builder()
                        .user(user)
                        .puzzle(puzzle)
                        .isLiked(true)
                        .likedAt(clock.instant())
                        .build()
        );
        puzzle.increaseLikedCount();

        return true;
    }

    @Transactional
    public boolean toggleDislike(Long puzzleId, UserEntity user) {
        CommunityPuzzle puzzle = communityPuzzleRepository.findById(puzzleId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));

        Optional<UserCommunityPuzzle> ucp = userCommunityPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), puzzleId);
        if (ucp.isPresent()) {
            if (ucp.get().isDisliked()) puzzle.decreaseDislikedCount();
            else {
                if (ucp.get().isLiked()) puzzle.decreaseLikedCount();
                puzzle.increaseDislikedCount();
            }
            return ucp.get().toggleDislike();
        }

        userCommunityPuzzleRepository.save(
                UserCommunityPuzzle.builder()
                        .user(user)
                        .puzzle(puzzle)
                        .isDisliked(true)
                        .build()
        );
        puzzle.increaseDislikedCount();

        return true;
    }

}
