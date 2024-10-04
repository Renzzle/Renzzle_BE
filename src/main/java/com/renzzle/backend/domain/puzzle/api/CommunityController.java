package com.renzzle.backend.domain.puzzle.api;

import com.renzzle.backend.domain.puzzle.api.request.AddCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.api.request.CommunityPuzzleResultUpdateRequest;
import com.renzzle.backend.domain.puzzle.api.response.AddPuzzleResponse;
import com.renzzle.backend.domain.puzzle.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.service.CommunityService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static com.renzzle.backend.global.util.BindingResultUtils.getErrorMessages;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@Tag(name = "Community Puzzle API", description = "Community Puzzle API")
public class CommunityController {

    private final CommunityService communityService;

    @PostMapping("/puzzle")
    public ApiResponse<AddPuzzleResponse> addCommunityPuzzle(
            @Valid @RequestBody AddCommunityPuzzleRequest request,
            @AuthenticationPrincipal UserDetailsImpl user,
            BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        return ApiUtils.success(communityService.addCommunityPuzzle(request, user.getUser()));
    }

    @PostMapping("/solve")
    public ApiResponse<Integer> solveCommunityPuzzle(
            @Valid @RequestBody CommunityPuzzleResultUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl user,
            BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        CommunityPuzzle puzzle = communityService.findCommunityPuzzleById(request.puzzleId());
        if(puzzle == null)
            throw new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE);

        int successCnt = communityService.solveCommunityPuzzle(puzzle, user.getUser());

        return ApiUtils.success(successCnt);
    }

    @PostMapping("/fail")
    public ApiResponse<Integer> failCommunityPuzzle(
            @Valid @RequestBody CommunityPuzzleResultUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl user,
            BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        CommunityPuzzle puzzle = communityService.findCommunityPuzzleById(request.puzzleId());
        if(puzzle == null)
            throw new CustomException(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE);

        int failCnt = communityService.failCommunityPuzzle(puzzle, user.getUser());

        return ApiUtils.success(failCnt);
    }

}
