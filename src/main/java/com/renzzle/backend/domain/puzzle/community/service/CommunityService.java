package com.renzzle.backend.domain.puzzle.community.service;

import com.renzzle.backend.domain.puzzle.community.api.request.AddCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.api.response.AddCommunityPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzleAnswerResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetSingleCommunityPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.*;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
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

import static com.renzzle.backend.global.common.constant.ItemPrice.HINT;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final Clock clock;
    private final CommunityPuzzleRepository communityPuzzleRepository;
    private final UserCommunityPuzzleRepository userCommunityPuzzleRepository;

    @Transactional
    public AddCommunityPuzzleResponse addCommunityPuzzle(AddCommunityPuzzleRequest request, UserEntity user) {
        String boardKey = BoardUtils.makeBoardKey(request.boardStatus());

        CommunityPuzzle puzzle = CommunityPuzzle.builder()
                .boardStatus(request.boardStatus())
                .boardKey(boardKey)
                .answer(request.answer())
                .depth(request.depth())
                .rating(request.depth() * 200.0) // TODO: rating formula
                .description(request.description())
                .user(user)
                .winColor(WinColor.getWinColor(request.winColor()))
                .build();

        CommunityPuzzle result = communityPuzzleRepository.save(puzzle);

        return AddCommunityPuzzleResponse.builder()
                .puzzleId(result.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public List<GetCommunityPuzzleResponse> getCommunityPuzzleList(GetCommunityPuzzleRequest request, UserEntity user) {
        List<CommunityPuzzle> puzzleList = communityPuzzleRepository.searchCommunityPuzzles(request, user.getId());

        List<GetCommunityPuzzleResponse> response = new ArrayList<>();
        for(CommunityPuzzle puzzle : puzzleList) {
            boolean isSolved = userCommunityPuzzleRepository.checkIsSolvedPuzzle(puzzle.getId(), user.getId());

            response.add(
                    GetCommunityPuzzleResponse.builder()
                            .id(puzzle.getId())
                            .boardStatus(puzzle.getBoardStatus())
                            .authorId(puzzle.getUser().getId())
                            .authorName(puzzle.getUser().getNickname())
                            .depth(puzzle.getDepth())
                            .winColor(puzzle.getWinColor().getName())
                            .likeCount(puzzle.getLikeCount())
                            .createdAt(puzzle.getCreatedAt().toString())
                            .isSolved(isSolved)
                            .isVerified(puzzle.getIsVerified())
                            .build()
            );
        }

        return response;
    }

    @Transactional(readOnly = true)
    public GetSingleCommunityPuzzleResponse getCommunityPuzzleById(Long puzzleId, UserEntity user) {
        CommunityPuzzle puzzle = communityPuzzleRepository.findById(puzzleId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));

        boolean isSolved = userCommunityPuzzleRepository.checkIsSolvedPuzzle(puzzle.getId(), user.getId());
        boolean myLike = userCommunityPuzzleRepository.getMyLike(user.getId(), puzzleId).orElse(false);
        boolean myDislike = userCommunityPuzzleRepository.getMyDislike(user.getId(), puzzleId).orElse(false);;

        return GetSingleCommunityPuzzleResponse.builder()
                .id(puzzle.getId())
                .boardStatus(puzzle.getBoardStatus())
                .authorId(puzzle.getUser().getId())
                .authorName(puzzle.getUser().getNickname())
                .depth(puzzle.getDepth())
                .winColor(puzzle.getWinColor().getName())
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
        CommunityPuzzle puzzle = communityPuzzleRepository.findById(puzzleId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));

        user.purchase(HINT.getPrice());

        return GetCommunityPuzzleAnswerResponse.builder()
                .answer(puzzle.getAnswer())
                .currency(user.getCurrency())
                .build();
    }

    @Transactional
    public void solveCommunityPuzzle(Long puzzleId, UserEntity user) {
        Optional<UserCommunityPuzzle> ucp = userCommunityPuzzleRepository.findByUser_IdAndPuzzle_Id(user.getId(), puzzleId);
        if (ucp.isPresent()) {
            ucp.get().solvePuzzle(clock);
            return;
        }

        CommunityPuzzle puzzle = communityPuzzleRepository.findById(puzzleId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));

        userCommunityPuzzleRepository.save(
                UserCommunityPuzzle.builder()
                        .user(user)
                        .puzzle(puzzle)
                        .isSolved(true)
                        .solvedAt(clock.instant())
                        .build()
        );
    }

}
