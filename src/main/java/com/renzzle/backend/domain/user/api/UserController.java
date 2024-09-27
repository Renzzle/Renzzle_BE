package com.renzzle.backend.domain.user.api;

import com.renzzle.backend.domain.user.api.request.UpdateLevelRequest;
import com.renzzle.backend.domain.user.api.response.SubscriptionResponse;
import com.renzzle.backend.domain.user.api.response.UserResponse;
import com.renzzle.backend.domain.user.service.UserService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/user")
@RequiredArgsConstructor
@Tag(name = "User API", description = "User management API")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Retrieve user information", description = "Get the details of the user info")
    @GetMapping
    public ApiResponse<UserResponse> getUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long UserId = userDetails.getUser().getId();
        UserResponse userResponse = userService.getUser(UserId);
        return ApiUtils.success(userResponse);
    }

    @Operation(summary = "Delete a user", description = "Delete the user")
    @DeleteMapping
    public ApiResponse<Long> deleteUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long deletedUserId = userService.deleteUser(userDetails.getUser().getId());
        return ApiUtils.success(deletedUserId);  // 삭제된 userId 반환
    }

    @Operation(summary = "Update user level", description = "Update the level of the currently logged-in user")
    @PatchMapping("/level")
    public ApiResponse<UserResponse> updateLevel(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody UpdateLevelRequest request) {

        Long userId = userDetails.getUser().getId();
        UserResponse userResponse = userService.updateUserLevel(userId, request.getLevel());
        return ApiUtils.success(userResponse);
    }

    @Operation(summary = "Retrieve user subscription list", description = "Retrieve the subscribed users by the user")
    @GetMapping
    public ApiResponse<List<SubscriptionResponse>> getUserSubscriptions(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        Long userId = userDetails.getUser().getId();
        List<SubscriptionResponse> subscriptionResponses = userService.getUserSubscriptions(userId, id, size);
        return ApiUtils.success(subscriptionResponses);  // List<SubscriptionResponse>로 응답
    }
}
