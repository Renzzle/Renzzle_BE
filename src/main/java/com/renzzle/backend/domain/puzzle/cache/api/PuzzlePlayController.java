package com.renzzle.backend.domain.puzzle.cache.api;

import com.renzzle.backend.domain.puzzle.cache.api.request.SavePuzzleRequest;
import com.renzzle.backend.domain.puzzle.cache.api.response.GetAiResponseResponse;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.domain.puzzle.cache.service.PuzzlePlayService;
import com.renzzle.backend.domain.puzzle.shared.util.MoveUtils;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/puzzle/cache")
@RequiredArgsConstructor
@Tag(name = "Puzzle Play API", description = "퍼즐 플레이 관련 API")
public class PuzzlePlayController {

    private final PuzzlePlayService puzzlePlayService;

    @Operation(summary = "퍼즐 저장", description = "보드 상태를 기반으로 AI 풀이를 생성하여 퍼즐에 저장")
    @PostMapping("/save")
    public ApiResponse<Void> savePuzzle(@Valid @RequestBody SavePuzzleRequest request) {
        puzzlePlayService.savePuzzle(request.puzzleType(), request.puzzleId(), request.currentBoardState());
        return ApiUtils.success(null);
    }

    @Operation(summary = "AI 응답 조회", description = "현재 보드 상태에 대한 AI의 다음 수를 반환")
    @GetMapping("/ai-response")
    public ApiResponse<GetAiResponseResponse> getAiResponse(
            @RequestParam PuzzleType puzzleType,
            @RequestParam Long puzzleId,
            @RequestParam String currentBoardState) {
        Integer move = puzzlePlayService.getAiResponse(puzzleType, puzzleId, currentBoardState);
        String position = (move != null) ? MoveUtils.convertMoveToPosition(move) : null;
        return ApiUtils.success(new GetAiResponseResponse(position));
    }
}
