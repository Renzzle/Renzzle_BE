package com.renzzle.backend.domain.user.api;

import com.renzzle.backend.domain.puzzle.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.api.response.GetCommunityPuzzleResponse;
import com.renzzle.backend.domain.user.api.request.ChangeNicknameRequest;
import com.renzzle.backend.domain.user.api.request.PuzzleLikeRequest;
import com.renzzle.backend.domain.user.api.response.ChangeNicknameResponse;
import com.renzzle.backend.domain.user.api.response.LikeResponse;
import com.renzzle.backend.domain.user.api.response.UserResponse;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.domain.user.service.UserService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.renzzle.backend.global.util.ErrorUtils.getErrorMessages;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User API", description = "User management API")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Retrieve user information", description = "Get the details of the user information")
    @GetMapping
    public ApiResponse<UserResponse> getUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        UserResponse userResponse = userService.getUserResponse(userDetails.getUser());
        return ApiUtils.success(userResponse);
    }

    @Operation(summary = "Delete the user", description = "Delete the user, but no actual data(soft delete)")
    @DeleteMapping
    public ApiResponse<Long> deleteUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long deletedUserId = userService.deleteUser(userDetails.getUser());
        return ApiUtils.success(deletedUserId);
    }

    @Operation(summary = "Change user nickname", description = "Change nickname & Return remaining currency")
    @PatchMapping("/nickname")
    public ApiResponse<ChangeNicknameResponse> changeNickname(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                              @RequestBody ChangeNicknameRequest request) {
        ChangeNicknameResponse response = userService.changeNickname(userDetails.getUser(), request.nickname());
        return ApiUtils.success(response);
    }

    @Operation(summary = "Subscribe or unsubscribe to a user", description = "change the subscription status of a user")
    @PostMapping("/like")
    public ApiResponse<Boolean> changeLikeStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody PuzzleLikeRequest request) {
        UserEntity user = userDetails.getUser();

        log.info("Received request to change like status for puzzleId: {}", request.puzzleId());

        boolean isLiked = userService.toggleLike(request.puzzleId(), user);

        log.info("Like status changed for puzzleId: {}. New status: {}", request.puzzleId(), isLiked);

        return ApiUtils.success(isLiked);
    }

    @Operation(summary = "Retrieve user like list", description = "Retrieve user like list")
    @GetMapping("/like")
    public ApiResponse<List<LikeResponse>> getUserLike(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        log.info("Received parameters - ID: {}, Size: {}", id, size);

        if (userDetails == null) {
            log.error("User details not found");
            throw new CustomException(ErrorCode.CANNOT_FIND_USER);
        }

        Long userId = userDetails.getUser().getId();
        List<LikeResponse> likeResponse = userService.getUserLike(userId, id, size);
        return ApiUtils.success(likeResponse);
    }

    @Operation(summary = "Get user puzzle data", description = "Return puzzle list for a user")
    @GetMapping("/{userId}/puzzle")
    public ApiResponse<List<GetCommunityPuzzleResponse>> getUserPuzzle(
            @PathVariable("userId") Long userId,
            @ModelAttribute GetCommunityPuzzleRequest request,
            BindingResult bindingResult
    ) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        int size = (request.size() != null) ? request.size() : 10;

        return ApiUtils.success(userService.getUserCommunityPuzzleList(userId, request.id(), size));
    }

    @Operation(summary = "Delete user puzzle", description = "Delete user puzzle")
    @DeleteMapping("/{puzzleId}")
    public ApiResponse<Object> deleteUserPuzzle(
            @PathVariable("puzzleId") Long puzzleId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UserEntity user = userDetails.getUser();
        userService.deleteUserPuzzle(puzzleId, user);
        return ApiUtils.success(null);
    }

}
