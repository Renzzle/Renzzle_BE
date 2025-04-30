package com.renzzle.backend.domain.puzzle.community.api;

import com.renzzle.backend.domain.puzzle.community.api.request.AddCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.api.response.AddCommunityPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzleAnswerResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzlesResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetSingleCommunityPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.service.CommunityService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@Tag(name = "Community Puzzle API", description = "Community Puzzle API")
public class CommunityController {

    private final CommunityService communityService;

    @Operation(summary = "Register new community puzzle", description = "Add new community puzzle")
    @PostMapping("/puzzle")
    public ApiResponse<AddCommunityPuzzleResponse> addCommunityPuzzle(
            @Valid @RequestBody AddCommunityPuzzleRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ApiUtils.success(communityService.addCommunityPuzzle(request, user.getUser()));
    }

    @Operation(summary = "Get community puzzle data", description = "Return community puzzle list")
    @GetMapping("/puzzle")
    public ApiResponse<List<GetCommunityPuzzlesResponse>> getCommunityPuzzles(
            @Valid @ModelAttribute GetCommunityPuzzleRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ApiUtils.success(communityService.getCommunityPuzzleList(request, user.getUser()));
    }

    @Operation(summary = "Get a single community puzzle data", description = "Return a specific community puzzle by ID")
    @GetMapping("/puzzle/{puzzleId}")
    public ApiResponse<GetSingleCommunityPuzzleResponse> getCommunityPuzzleById(
            @PathVariable Long puzzleId,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ApiUtils.success(communityService.getCommunityPuzzleById(puzzleId, user.getUser()));
    }

    @Operation(summary = "Get community puzzle answer", description = "Return community puzzle answer & remaining currency")
    @PostMapping("/puzzle/{puzzleId}/answer")
    public ApiResponse<GetCommunityPuzzleAnswerResponse> getCommunityPuzzleAnswer(
            @PathVariable Long puzzleId,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ApiUtils.success(communityService.getCommunityPuzzleAnswer(puzzleId, user.getUser()));
    }

    @Operation(summary = "Solve community puzzle", description = "Save the information that a user has solved puzzle")
    @PostMapping("/puzzle/{puzzleId}/solve")
    public ApiResponse<Void> solveCommunityPuzzle(
            @PathVariable Long puzzleId,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        communityService.solveCommunityPuzzle(puzzleId, user.getUser());
        return ApiUtils.success(null);
    }

    @Operation(summary = "Like community puzzle", description = "Registers a like to the puzzle by the user")
    @PostMapping("/puzzle/{puzzleId}/like")
    public ApiResponse<Boolean> likePuzzle(
            @PathVariable Long puzzleId,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ApiUtils.success(communityService.toggleLike(puzzleId, user.getUser()));
    }

    @Operation(summary = "Dislike community puzzle", description = "Registers a dislike to the puzzle by the user")
    @PostMapping("/puzzle/{puzzleId}/dislike")
    public ApiResponse<Boolean> dislikePuzzle(
            @PathVariable Long puzzleId,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        return ApiUtils.success(communityService.toggleDislike(puzzleId, user.getUser()));
    }

}
