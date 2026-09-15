package com.renzzle.backend.domain.puzzle.cache.api;

import com.renzzle.backend.domain.puzzle.cache.api.request.GetCommunityPuzzlesForCacheRequest;
import com.renzzle.backend.domain.puzzle.cache.api.request.GetNextMovesRequest;
import com.renzzle.backend.domain.puzzle.cache.api.request.SavePuzzleRequest;
import com.renzzle.backend.domain.puzzle.cache.api.response.CommunityPuzzleCachePickerResponse;
import com.renzzle.backend.domain.puzzle.cache.api.response.GetAiResponseResponse;
import com.renzzle.backend.domain.puzzle.cache.api.response.NextMoveCandidateResponse;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.domain.puzzle.cache.service.PuzzleCacheService;
import com.renzzle.backend.domain.puzzle.community.service.CommunityService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/puzzle/cache")
@RequiredArgsConstructor
@Tag(name = "Puzzle Cache API", description = "Puzzle cache API")
public class PuzzleCacheController {

    private final PuzzleCacheService puzzleCacheService;
    private final CommunityService communityService;

    @Operation(summary = "Community puzzle candidates (for cache entry)", description = "Filters by exact author nickname, win color and depth range")
    @GetMapping("/community-puzzles")
    public ApiResponse<List<CommunityPuzzleCachePickerResponse>> getCommunityPuzzlesForCache(
            @Valid @ParameterObject @ModelAttribute GetCommunityPuzzlesForCacheRequest request
    ) {
        return ApiUtils.success(communityService.getCommunityPuzzlesForCachePicker(request));
    }

    @Operation(summary = "Save puzzle", description = "Stores the given board state and answer move into the puzzle cache")
    @PostMapping("/save")
    public ApiResponse<Void> savePuzzle(@Valid @RequestBody SavePuzzleRequest request) {
        puzzleCacheService.savePuzzle(request.puzzleType(), request.puzzleId(), request.currentBoardState(), request.answerPuzzle());
        return ApiUtils.success(null);
    }

    @Operation(summary = "Get AI response", description = "Returns the AI's next move for the current board state")
    @GetMapping("/ai-response")
    public ApiResponse<GetAiResponseResponse> getAiResponse(
            @RequestParam PuzzleType puzzleType,
            @RequestParam Long puzzleId,
            @RequestParam String currentBoardState) {
        Integer move = puzzleCacheService.getAiResponse(puzzleType, puzzleId, currentBoardState);
        String position = (move != null) ? moveIndexToPosition(move) : null;
        return ApiUtils.success(new GetAiResponseResponse(position));
    }

    @Operation(
            summary = "Get next move candidates",
            description = "Takes a board state on the user's turn and returns every cached user move with its AI response"
    )
    @GetMapping("/next-moves")
    public ApiResponse<List<NextMoveCandidateResponse>> getNextMoves(
            @Valid @ParameterObject @ModelAttribute GetNextMovesRequest request
    ) {
        Map<Integer, Integer> candidates = puzzleCacheService.getNextMoveCandidates(
                request.puzzleType(), request.puzzleId(), request.userTurnBoardState());

        List<NextMoveCandidateResponse> response = new ArrayList<>(candidates.size());
        for (Map.Entry<Integer, Integer> candidate : candidates.entrySet()) {
            response.add(new NextMoveCandidateResponse(
                    moveIndexToPosition(candidate.getKey()),
                    moveIndexToPosition(candidate.getValue())
            ));
        }
        return ApiUtils.success(response);
    }

    private static final int BOARD_SIZE = 15;

    private static String moveIndexToPosition(int move) {
        char letter = (char) ('a' + move / BOARD_SIZE);
        int number = move % BOARD_SIZE + 1;
        return "" + letter + number;
    }
}
