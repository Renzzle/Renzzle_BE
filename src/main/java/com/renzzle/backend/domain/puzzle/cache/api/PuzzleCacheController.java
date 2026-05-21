package com.renzzle.backend.domain.puzzle.cache.api;

import com.renzzle.backend.domain.puzzle.cache.api.request.GetCommunityPuzzlesForCacheRequest;
import com.renzzle.backend.domain.puzzle.cache.api.request.SavePuzzleRequest;
import com.renzzle.backend.domain.puzzle.cache.api.response.CommunityPuzzleCachePickerResponse;
import com.renzzle.backend.domain.puzzle.cache.api.response.GetAiResponseResponse;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.domain.puzzle.cache.service.PuzzleCacheService;
import com.renzzle.backend.domain.puzzle.community.service.CommunityService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/puzzle/cache")
@RequiredArgsConstructor
@Tag(name = "Puzzle Cache API", description = "퍼즐 캐시 관련 API")
public class PuzzleCacheController {

    private final PuzzleCacheService puzzleCacheService;
    private final CommunityService communityService;

    @Operation(summary = "커뮤니티 퍼즐 후보 목록 (캐시 입력용)", description = "작성자 닉네임 완전 일치·승리 색·깊이 구간 필터")
    @GetMapping("/community-puzzles")
    public ApiResponse<List<CommunityPuzzleCachePickerResponse>> getCommunityPuzzlesForCache(
            @Valid @ModelAttribute GetCommunityPuzzlesForCacheRequest request
    ) {
        return ApiUtils.success(communityService.getCommunityPuzzlesForCachePicker(request));
    }

    @Operation(summary = "퍼즐 저장", description = "보드 상태와 정답 수를 전달받아 퍼즐에 저장")
    @PostMapping("/save")
    public ApiResponse<Void> savePuzzle(@Valid @RequestBody SavePuzzleRequest request) {
        puzzleCacheService.savePuzzle(request.puzzleType(), request.puzzleId(), request.currentBoardState(), request.answerPuzzle());
        return ApiUtils.success(null);
    }

    @Operation(summary = "AI 응답 조회", description = "현재 보드 상태에 대한 AI의 다음 수를 반환")
    @GetMapping("/ai-response")
    public ApiResponse<GetAiResponseResponse> getAiResponse(
            @RequestParam PuzzleType puzzleType,
            @RequestParam Long puzzleId,
            @RequestParam String currentBoardState) {
        Integer move = puzzleCacheService.getAiResponse(puzzleType, puzzleId, currentBoardState);
        String position = (move != null) ? moveIndexToPosition(move) : null;
        return ApiUtils.success(new GetAiResponseResponse(position));
    }

    private static final int BOARD_SIZE = 15;

    private static String moveIndexToPosition(int move) {
        char letter = (char) ('a' + move / BOARD_SIZE);
        int number = move % BOARD_SIZE + 1;
        return "" + letter + number;
    }
}
