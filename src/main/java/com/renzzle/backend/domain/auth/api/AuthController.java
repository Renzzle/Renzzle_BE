package com.renzzle.backend.domain.auth.api;

import com.renzzle.backend.domain.auth.api.request.AuthEmailRequest;
import com.renzzle.backend.domain.auth.api.request.ConfirmCodeRequest;
import com.renzzle.backend.domain.auth.api.response.AuthEmailResponse;
import com.renzzle.backend.domain.auth.api.response.ConfirmCodeResponse;
import com.renzzle.backend.domain.auth.service.AccountService;
import com.renzzle.backend.domain.auth.service.EmailService;
import com.renzzle.backend.global.common.ApiResponse;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static com.renzzle.backend.global.util.BindingResultUtils.getErrorMessages;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth API", description = "auth & account api")
public class AuthController {

    private final EmailService emailService;
    private final AccountService accountService;

    @Operation(summary = "Confirm email", description = "Send confirm code to user email")
    @PostMapping("/email")
    public ApiResponse<AuthEmailResponse> sendCode(@Valid @RequestBody AuthEmailRequest request, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        int count = emailService.getRequestCount(request.email());
        if(count >= 5) {
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

    @Operation(summary = "Confirm code", description = "Authenticate code")
    @PostMapping("/confirmCode")
    public ApiResponse<ConfirmCodeResponse> confirmCode(@Valid @RequestBody ConfirmCodeRequest request, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            throw new ValidationException(getErrorMessages(bindingResult));
        }

        boolean isCorrect = emailService.confirmCode(request.email(), request.code());
        if(!isCorrect) throw new CustomException(ErrorCode.NOT_VALID_CODE);

        String authVerityToken = accountService.createAuthVerityToken(request.email());

        ConfirmCodeResponse response = ConfirmCodeResponse
                .builder()
                .authVerityToken(authVerityToken)
                .build();

        return ApiUtils.success(response);
    }

}
