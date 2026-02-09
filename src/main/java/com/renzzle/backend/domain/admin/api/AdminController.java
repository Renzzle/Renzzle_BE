package com.renzzle.backend.domain.admin.api;

import com.renzzle.backend.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin dashboard page (requires admin token)")
public class AdminController {

    /**
     * 테스트용 어드민 로그인 페이지
     * - 인증 없이 접근 가능
     * - 로그인 폼 제공 → 로그인 후 admin이면 dashboard로 이동
     */
    @Operation(
            summary = "Admin login page (for testing)",
            description = "Returns admin login page. No authentication required. After login, admin users can access dashboard."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login page HTML",
                    content = @Content(mediaType = MediaType.TEXT_HTML_VALUE))
    })
    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    /**
     * 어드민 대시보드 (실배포용)
     * - JWT 필터에서 토큰 검증 후 SecurityContext 설정
     * - SecurityConfig에서 hasAuthority(ADMIN)으로 admin만 접근 허용
     * - admin이 아닌 경우 403 (CustomAccessDeniedHandler)
     */
    @Operation(
            summary = "Admin dashboard (production)",
            description = "Returns admin dashboard page. Same auth as other APIs: JWT in 'Authorization: Bearer <token>'. Only users with admin authority can access."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dashboard HTML",
                    content = @Content(mediaType = MediaType.TEXT_HTML_VALUE)),
            @ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    })
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/dashboard")
    public String dashboard(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(hidden = true) Model model
    ) {
        model.addAttribute("userEmail", userDetails.getUser().getEmail());
        return "admin/dashboard";
    }
}
