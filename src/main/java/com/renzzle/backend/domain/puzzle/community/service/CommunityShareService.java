package com.renzzle.backend.domain.puzzle.community.service;

import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.domain.puzzle.cache.service.PuzzleCacheService;
import com.renzzle.backend.domain.puzzle.community.api.response.GetSharedPuzzleIdResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetSharedPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.ShareCommunityPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.community.util.SharedMovesEncoder;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommunityShareService {

    private final CommunityPuzzleRepository communityPuzzleRepository;
    private final PuzzleCacheService puzzleCacheService;

    @Value("${community.puzzle.share-base-url}")
    private String shareBaseUrl;

    // Locked, so two first shares at once settle on one token
    @Transactional
    public ShareCommunityPuzzleResponse shareCommunityPuzzle(Long puzzleId) {
        CommunityPuzzle puzzle = communityPuzzleRepository.findByIdForUpdate(puzzleId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));
        puzzle.issueShareToken();
        return new ShareCommunityPuzzleResponse(shareBaseUrl + "/puzzle/" + puzzle.getShareToken());
    }

    @Transactional(readOnly = true)
    public GetSharedPuzzleIdResponse getSharedPuzzleId(String token) {
        return new GetSharedPuzzleIdResponse(findPuzzleId(token));
    }

    // Public, so it leaves out the answer and IDs and isn't counted as a view
    @Transactional(readOnly = true)
    public GetSharedPuzzleResponse getSharedPuzzle(String token) {
        CommunityPuzzle puzzle = communityPuzzleRepository.findByShareToken(token)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));

        return GetSharedPuzzleResponse.builder()
                .boardStatus(puzzle.getBoardStatus())
                .authorName(puzzle.getUser().getNickname())
                .description(puzzle.getDescription())
                .depth(puzzle.getDepth())
                .winColor(puzzle.getWinColor().getName())
                .isVerified(puzzle.getIsVerified())
                .build();
    }

    @Transactional(readOnly = true)
    public String getSharedNextMoves(String token, String userTurnBoardState) {
        Map<Integer, Integer> candidates = puzzleCacheService.getNextMoveCandidates(
                PuzzleType.COMMUNITY, findPuzzleId(token), userTurnBoardState);
        return SharedMovesEncoder.encode(candidates, token, userTurnBoardState);
    }

    private Long findPuzzleId(String token) {
        return communityPuzzleRepository.findIdByShareToken(token)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE));
    }
}
