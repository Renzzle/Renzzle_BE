package com.renzzle.backend.domain.puzzle.community.api;

import com.renzzle.backend.domain.puzzle.community.api.request.AddCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.api.request.CommunityPuzzleResultUpdateRequest;
import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.api.response.AddPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.service.CommunityService;
import com.renzzle.backend.global.common.constant.SortOption;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.renzzle.backend.global.util.ErrorUtils.getErrorMessages;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@Tag(name = "Community Puzzle API", description = "Community Puzzle API")
public class CommunityController {

    private final CommunityService communityService;

    @Operation(summary = "Get community puzzle data", description = "Return community puzzle list")
    @GetMapping("/puzzle")
    public ApiResponse<List<GetCommunityPuzzleResponse>> getCommunityPuzzle(
            @ModelAttribute GetCommunityPuzzleRequest request,
            BindingResult bindingResult
    ) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        int size = (request.size() != null) ? request.size() : 10;
        String sort = (request.sort() != null) ? request.sort() : SortOption.RECOMMEND.name();
        SortOption sortOption = SortOption.valueOf(sort);

        return ApiUtils.success(communityService.getCommunityPuzzleList(request.id(), size, sortOption));
    }

    @Operation(summary = "Register new community puzzle", description = "Add new community puzzle")
    @PostMapping("/puzzle")
    public ApiResponse<AddPuzzleResponse> addCommunityPuzzle(
            @Valid @RequestBody AddCommunityPuzzleRequest request,
            @AuthenticationPrincipal UserDetailsImpl user,
            BindingResult bindingResult
    ) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        return ApiUtils.success(communityService.addCommunityPuzzle(request, user.getUser()));
    }

    @Operation(summary = "Solve community puzzle", description = "Solve count increase & Apply correct rate")
    @PostMapping("/solve")
    public ApiResponse<Integer> solveCommunityPuzzle(
            @Valid @RequestBody CommunityPuzzleResultUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl user,
            BindingResult bindingResult
    ) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        int successCnt = communityService.solveCommunityPuzzle(request.puzzleId(), user.getUser());

        return ApiUtils.success(successCnt);
    }

    @Operation(summary = "Fail community puzzle", description = "Fail count increase & Apply correct rate")
    @PostMapping("/fail")
    public ApiResponse<Integer> failCommunityPuzzle(
            @Valid @RequestBody CommunityPuzzleResultUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl user,
            BindingResult bindingResult
    ) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        int failCnt = communityService.failCommunityPuzzle(request.puzzleId(), user.getUser());

        return ApiUtils.success(failCnt);
    }

    @Operation(summary = "Search community puzzle", description = "Return community puzzle list according to query")
    @GetMapping("/search")
    public ApiResponse<List<GetCommunityPuzzleResponse>> searchCommunityPuzzle(@RequestParam("query") String query) {
        return ApiUtils.success(communityService.searchCommunityPuzzle(query));
    }

}
