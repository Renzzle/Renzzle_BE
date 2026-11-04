package com.renzzle.backend.domain.puzzle.community.api;

import com.renzzle.backend.domain.puzzle.community.api.request.GetSharedNextMovesRequest;
import com.renzzle.backend.domain.puzzle.community.api.response.GetSharedPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.service.CommunityShareService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Outside /api, so the share page calls it without the app key or a login
@RestController
@RequestMapping("/puzzle/{token}")
@RequiredArgsConstructor
@Tag(name = "Shared Puzzle API", description = "Public read-only API behind the puzzle share page")
public class SharedPuzzleController {

    private final CommunityShareService communityShareService;

    @Operation(summary = "Get shared puzzle", description = "Board and display info only; no answer, and not counted as a view")
    @GetMapping("/data")
    public ApiResponse<GetSharedPuzzleResponse> getSharedPuzzle(@PathVariable String token) {
        return ApiUtils.success(communityShareService.getSharedPuzzle(token));
    }

    @Operation(summary = "Get shared puzzle next moves", description = "Cached user moves and AI replies for a user-to-move board, encoded")
    @GetMapping("/next-moves")
    public ApiResponse<String> getSharedNextMoves(
            @PathVariable String token,
            @Valid @ParameterObject @ModelAttribute GetSharedNextMovesRequest request
    ) {
        return ApiUtils.success(communityShareService.getSharedNextMoves(token, request.board()));
    }
}
