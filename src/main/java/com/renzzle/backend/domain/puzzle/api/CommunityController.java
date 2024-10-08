package com.renzzle.backend.domain.puzzle.api;

import com.renzzle.backend.domain.puzzle.api.request.AddCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.api.request.CommunityPuzzleResultUpdateRequest;
import com.renzzle.backend.domain.puzzle.api.response.AddPuzzleResponse;
import com.renzzle.backend.domain.puzzle.api.response.GetCommunityPuzzleResponse;
import com.renzzle.backend.domain.puzzle.service.CommunityService;
import com.renzzle.backend.global.common.constant.SortOption;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import com.renzzle.backend.global.validation.ValidEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.renzzle.backend.global.util.BindingResultUtils.getErrorMessages;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@Tag(name = "Community Puzzle API", description = "Community Puzzle API")
public class CommunityController {

    private final CommunityService communityService;

    @Operation(summary = "Get community puzzle data", description = "Return community puzzle list")
    @GetMapping("/puzzle")
    public ApiResponse<List<GetCommunityPuzzleResponse>> getCommunityPuzzle(
            @RequestParam(required = false) Long id,

            @Min(value = 1, message = "size는 최소 1이어야 합니다")
            @Max(value = 100, message = "size는 최대 100이어야 합니다")
            @RequestParam(defaultValue = "10") Integer size,

            @ValidEnum(enumClass = SortOption.class, message = "잘못된 sort 형식입니다")
            @RequestParam(defaultValue = "RECOMMEND") String sort,

            BindingResult bindingResult
    ) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        SortOption sortOption = SortOption.valueOf(sort);

        return ApiUtils.success(communityService.getCommunityPuzzleList(id, size, sortOption));
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

}
