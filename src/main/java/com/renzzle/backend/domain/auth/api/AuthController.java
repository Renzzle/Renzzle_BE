package com.renzzle.backend.domain.auth.api;

import com.renzzle.backend.domain.auth.api.request.*;
import com.renzzle.backend.domain.auth.api.response.AuthEmailResponse;
import com.renzzle.backend.domain.auth.api.response.ConfirmCodeResponse;
import com.renzzle.backend.domain.auth.api.response.LoginResponse;
import com.renzzle.backend.domain.auth.service.AccountService;
import com.renzzle.backend.domain.auth.service.AuthService;
import com.renzzle.backend.domain.auth.service.EmailService;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
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
import static com.renzzle.backend.domain.auth.service.EmailService.EMAIL_VERIFICATION_LIMIT;
import static com.renzzle.backend.global.util.ErrorUtils.getErrorMessages;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth API", description = "Auth & Account API")
public class AuthController {

    private final EmailService emailService;
    private final AccountService accountService;
    private final AuthService authService;

    @Operation(summary = "Confirm email", description = "Send confirm code to user email")
    @PostMapping("/email")
    public ApiResponse<AuthEmailResponse> sendCode(@Valid @RequestBody AuthEmailRequest request) {
        return ApiUtils.success(emailService.sendCode(request));
    }

    @Operation(summary = "Confirm code", description = "Authenticate code & Return token for sign up")
    @PostMapping("/confirmCode")
    public ApiResponse<ConfirmCodeResponse> confirmCode(@Valid @RequestBody ConfirmCodeRequest request) {
        return ApiUtils.success(emailService.confirmCode(request));
    }

    @Operation(summary = "Check duplication of nickname", description = "Return true if nickname exists")
    @GetMapping("/duplicate/{nickname}")
    public ApiResponse<Boolean> isDuplicateNickname(@PathVariable("nickname") String nickname) {
        return ApiUtils.success(accountService.isDuplicateNickname(nickname));
    }

    @Operation(summary = "Create new account", description = "Create new account & Issue authentication tokens for server access")
    @PostMapping("/signup")
    public ApiResponse<LoginResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiUtils.success(accountService.signUp(request));
    }

    @Operation(summary = "Login to service", description = "Issue authentication tokens for server access")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiUtils.success(accountService.login(request));
    }

    @Operation(summary = "Logout to service", description = "Delete the refresh token to prevent reissuing the authentication tokens")
    @PostMapping("/logout")
    public ApiResponse<Long> logout(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long id = authService.deleteRefreshToken(userDetails.getUser());
        return ApiUtils.success(id);
    }

    @Operation(summary = "Reissue authentication tokens", description = "Issue authentication tokens for server access")
    @PostMapping("/reissueToken")
    public ApiResponse<LoginResponse> reissueToken(@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestBody ReissueTokenRequest request) {
        return ApiUtils.success(authService.reissueToken(userDetails.getUser(), request));
    }

}
