package com.renzzle.backend.domain.puzzle.service;

import com.renzzle.backend.domain.puzzle.api.request.AddCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.api.response.AddPuzzleResponse;
import com.renzzle.backend.domain.puzzle.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.domain.UserCommunityPuzzle;
import com.renzzle.backend.domain.puzzle.domain.WinColor;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.util.BoardUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityPuzzleRepository communityPuzzleRepository;
    private final UserCommunityPuzzleRepository userCommunityPuzzleRepository;

    @Transactional
    public AddPuzzleResponse addCommunityPuzzle(AddCommunityPuzzleRequest request, UserEntity user) {
        String boardKey = BoardUtils.makeBoardKey(request.boardStatus());

        CommunityPuzzle puzzle = CommunityPuzzle.builder()
                .title(request.title())
                .boardStatus(request.boardStatus())
                .boardKey(boardKey)
                .depth(request.depth())
                .user(user)
                .difficulty(Difficulty.getDifficulty(request.difficulty()))
                .winColor(WinColor.getWinColor(request.winColor()))
                .build();

        CommunityPuzzle result = communityPuzzleRepository.save(puzzle);

        return AddPuzzleResponse.builder()
                .id(result.getId())
                .title(result.getTitle())
                .build();
    }

    @Transactional(readOnly = true)
    public CommunityPuzzle findCommunityPuzzleById(Long puzzleId) {
        Optional<CommunityPuzzle> puzzle = communityPuzzleRepository.findById(puzzleId);
        return puzzle.orElse(null);
    }

    @Transactional
    public int solveCommunityPuzzle(Long puzzleId, UserEntity user) {
        CommunityPuzzle puzzle = findCommunityPuzzleById(puzzleId);
        if(puzzle == null)
            throw new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE);

        Optional<UserCommunityPuzzle> findResult = userCommunityPuzzleRepository.findUserPuzzleInfo(user.getId(), puzzle.getId());
        UserCommunityPuzzle userPuzzleInfo = findResult.orElseGet(() -> userCommunityPuzzleRepository.save(
                UserCommunityPuzzle.builder()
                .user(user)
                .puzzle(puzzle)
                .build()));

        puzzle.addSolve();

        return userPuzzleInfo.addSolve();
    }

    @Transactional
    public int failCommunityPuzzle(Long puzzleId, UserEntity user) {
        CommunityPuzzle puzzle = findCommunityPuzzleById(puzzleId);
        if(puzzle == null)
            throw new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE);

        Optional<UserCommunityPuzzle> findResult = userCommunityPuzzleRepository.findUserPuzzleInfo(user.getId(), puzzle.getId());
        UserCommunityPuzzle userPuzzleInfo = findResult.orElseGet(() -> userCommunityPuzzleRepository.save(
                UserCommunityPuzzle.builder()
                        .user(user)
                        .puzzle(puzzle)
                        .build()));

        puzzle.addFail();

        return userPuzzleInfo.addFail();
    }

}
