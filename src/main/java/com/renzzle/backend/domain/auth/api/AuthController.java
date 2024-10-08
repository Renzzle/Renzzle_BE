package com.renzzle.backend.domain.auth.api;

import com.renzzle.backend.domain.auth.api.request.*;
import com.renzzle.backend.domain.auth.api.response.AuthEmailResponse;
import com.renzzle.backend.domain.auth.api.response.ConfirmCodeResponse;
import com.renzzle.backend.domain.auth.api.response.LoginResponse;
import com.renzzle.backend.domain.auth.service.AccountService;
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

    @Operation(summary = "Confirm email", description = "Send confirm code to user email")
    @PostMapping("/email")
    public ApiResponse<AuthEmailResponse> sendCode(@Valid @RequestBody AuthEmailRequest request, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        if(accountService.isDuplicatedEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        int count = emailService.getRequestCount(request.email());
        if(count >= EMAIL_VERIFICATION_LIMIT) {
            throw new CustomException(ErrorCode.EXCEED_EMAIL_AUTH_REQUEST);
        }

        String code = emailService.sendAuthEmail(request.email());
        emailService.saveConfirmCode(request.email(), code, count);

        return ApiUtils.success(AuthEmailResponse
                .builder()
                .code(code)
                .requestCount(count + 1)
                .build());
    }

    @Operation(summary = "Confirm code", description = "Authenticate code & Return token for sign up")
    @PostMapping("/confirmCode")
    public ApiResponse<ConfirmCodeResponse> confirmCode(@Valid @RequestBody ConfirmCodeRequest request, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        boolean isCorrect = emailService.confirmCode(request.email(), request.code());
        if(!isCorrect) throw new CustomException(ErrorCode.INVALID_EMAIL_AUTH_CODE);

        String authVerityToken = accountService.createAuthVerityToken(request.email());

        ConfirmCodeResponse response = ConfirmCodeResponse
                .builder()
                .authVerityToken(authVerityToken)
                .build();

        return ApiUtils.success(response);
    }

    @Operation(summary = "Check duplication of nickname", description = "Return true if nickname exists")
    @GetMapping("/duplicate/{nickname}")
    public ApiResponse<Boolean> isDuplicateNickname(@PathVariable("nickname") String nickname) {
        return ApiUtils.success(accountService.isDuplicateNickname(nickname));
    }

    @Operation(summary = "Create new account", description = "Create new account & Issue authentication tokens for server access")
    @PostMapping("/signup")
    public ApiResponse<LoginResponse> signup(@Valid @RequestBody SignupRequest request, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        if(!accountService.verifyAuthVerityToken(request.authVerityToken(), request.email())) {
            throw new CustomException(ErrorCode.INVALID_AUTH_VERITY_TOKEN);
        }

        UserEntity user = accountService.createNewUser(request.email(), request.password(), request.nickname());

        return ApiUtils.success(accountService.createAuthTokens(user.getId()));
    }

    @Operation(summary = "Login to service", description = "Issue authentication tokens for server access")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        long userId = accountService.verifyLoginInfo(request.email(), request.password());

        return ApiUtils.success(accountService.createAuthTokens(userId));
    }

    @Operation(summary = "Logout to service", description = "Delete the refresh token to prevent reissuing the authentication tokens")
    @PostMapping("/logout")
    public ApiResponse<Long> logout(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long id = accountService.deleteRefreshToken(userDetails.getUser().getId());
        return ApiUtils.success(id);
    }

    @Operation(summary = "Reissue authentication tokens", description = "Issue authentication tokens for server access")
    @PostMapping("/reissueToken")
    public ApiResponse<LoginResponse> reissueToken(@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestBody ReissueTokenRequest request) {
        if(!accountService.verifyRefreshToken(request.refreshToken()))
            throw new CustomException(ErrorCode.EXPIRED_JWT_TOKEN);

        return ApiUtils.success(accountService.createAuthTokens(userDetails.getUser().getId()));
    }

}
