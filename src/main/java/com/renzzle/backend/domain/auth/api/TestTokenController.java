package com.renzzle.backend.domain.auth.api;

import com.renzzle.backend.domain.auth.api.request.TestTokenRequest;
import com.renzzle.backend.domain.auth.api.response.TestTokenResponse;
import com.renzzle.backend.domain.auth.service.TestTokenService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "docs.enabled", havingValue = "true")
@Tag(name = "Test Token API", description = "Test helper, mapped only while DOCS_ENABLED is true")
public class TestTokenController {

    private final TestTokenService testTokenService;

    @Operation(summary = "Issue a 7-day test token", description = "Verify the account credentials and issue a 7 day access token")
    @PostMapping("/test-token")
    public ApiResponse<TestTokenResponse> createTestToken(@Valid @RequestBody TestTokenRequest request) {
        return ApiUtils.success(testTokenService.createTestToken(request));
    }

}
