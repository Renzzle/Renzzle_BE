package com.renzzle.backend.domain.puzzle.service;

import com.renzzle.backend.domain.puzzle.api.request.AddCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.api.response.AddPuzzleResponse;
import com.renzzle.backend.domain.puzzle.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.domain.WinColor;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.util.BoardUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityPuzzleRepository communityPuzzleRepository;
    private final UserCommunityPuzzleRepository userCommunityPuzzleRepository;

    public AddPuzzleResponse addCommunityPuzzle(AddCommunityPuzzleRequest request, UserEntity user) {
        String boardKey = BoardUtils.makeBoardKey(request.boardStatus());

        CommunityPuzzle puzzle = CommunityPuzzle.builder()
                .title(request.title())
                .boardStatus(request.boardStatus())
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

}
