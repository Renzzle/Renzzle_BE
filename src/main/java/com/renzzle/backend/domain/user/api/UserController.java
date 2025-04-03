package com.renzzle.backend.domain.user.api;

import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzlesResponse;
import com.renzzle.backend.domain.user.api.request.ChangeNicknameRequest;
import com.renzzle.backend.domain.user.api.response.ChangeNicknameResponse;
import com.renzzle.backend.domain.user.api.response.GetUserLikedPuzzlesResponse;
import com.renzzle.backend.domain.user.api.response.UserResponse;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.domain.user.service.UserService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return ApiUtils.success(userService.getUserResponse(userDetails.getUser()));
    }

    @Operation(summary = "Delete the user", description = "Delete the user, but no actual data(soft delete)")
    @DeleteMapping
    public ApiResponse<Long> deleteUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ApiUtils.success(userService.deleteUser(userDetails.getUser()));
    }

    @Operation(summary = "Change user nickname", description = "Change nickname & Return remaining currency")
    @PatchMapping("/nickname")
    public ApiResponse<ChangeNicknameResponse> changeNickname(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                              @RequestBody ChangeNicknameRequest request) {
        return ApiUtils.success(userService.changeNickname(userDetails.getUser(), request.nickname()));
    }

    @Operation(summary = "Retrieve user like list", description = "Retrieve user like list")
    @GetMapping("/like")
    public ApiResponse<List<GetCommunityPuzzlesResponse>> getUserLikedPuzzles(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(value = "id", required = false) Long cursorId,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        return ApiUtils.success(userService.getUserLikedPuzzleList(userDetails.getUser(), cursorId, size));
    }

    @Operation(summary = "Get user puzzle data", description = "Return puzzle list for a user")
    @GetMapping("/puzzle")
    public ApiResponse<List<GetCommunityPuzzlesResponse>> getUserPuzzles(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(value = "id", required = false) Long cursorId,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        return ApiUtils.success(userService.getUserPuzzleList(userDetails.getUser(), cursorId, size));
    }

    @Operation(summary = "Delete user puzzle", description = "Delete user puzzle")
    @DeleteMapping("/{puzzleId}")
    public ApiResponse<Long> deleteUserPuzzle(
            @PathVariable("puzzleId") Long puzzleId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ApiUtils.success(userService.deleteUserPuzzle(userDetails.getUser(), puzzleId));
    }

}
