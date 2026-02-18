package com.renzzle.backend.domain.admin.api;

import com.renzzle.backend.domain.auth.api.request.LoginRequest;
import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.service.AccountService;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.puzzle.training.api.request.GetTrainingPackRequest;
import com.renzzle.backend.domain.puzzle.training.api.response.GetPackResponse;
import com.renzzle.backend.domain.puzzle.training.api.response.GetTrainingPuzzleResponse;
import com.renzzle.backend.domain.puzzle.training.service.TrainingService;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin login and dashboard page")
public class AdminController {

    private final AccountService accountService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final TrainingService trainingService;
    private final Clock clock;

    /**
     * 어드민 로그인 페이지
     */
    @Operation(summary = "Admin login page", description = "Returns admin login page.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login page HTML",
                    content = @Content(mediaType = MediaType.TEXT_HTML_VALUE))
    })
    @GetMapping
    public String loginPage() {
        return "admin/login";
    }

    /**
     * 어드민 전용 로그인 API (12시간 토큰)
     */
    @Operation(summary = "Admin login", description = "Admin-specific login that issues 12-hour access token.")
    @SecurityRequirement(name = "Authorization")
    @PostMapping("/login")
    @ResponseBody
    public ApiResponse<AdminLoginResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        // 일반 로그인 처리
        var loginResponse = accountService.login(request);
        
        // admin 확인
        Long userId = jwtProvider.getUserId(loginResponse.accessToken());
        Optional<UserEntity> user = userRepository.findById(userId);
        if (user.isEmpty() || !adminRepository.existsByUser(user.get())) {
            throw new CustomException(ErrorCode.ADMIN_ACCESS_DENIED);
        }

        // admin 전용 토큰 (12시간) 재발급
        String adminAccessToken = jwtProvider.createAdminAccessToken(userId);
        Instant expiredAt = clock.instant().plus(12, ChronoUnit.HOURS);

        return ApiUtils.success(new AdminLoginResponse(
                "Bearer",
                adminAccessToken,
                expiredAt
        ));
    }

    /**
     * 어드민 대시보드
     */
    @Operation(summary = "Admin dashboard", description = "Admin dashboard page (requires admin token from /admin/login)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard HTML",
                    content = @Content(mediaType = MediaType.TEXT_HTML_VALUE))
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

    /**
     * 어드민 로그아웃
     * - 브라우저 쿠키에 저장된 admin_accessToken 삭제
     * - 로그인 페이지(/admin)로 리다이렉트
     */
    @Operation(summary = "Admin logout", description = "Clear admin_accessToken cookie and redirect to login page.")
    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("admin_accessToken", null);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 즉시 만료
        response.addCookie(cookie);
        return "redirect:/admin";
    }

    /**
     * Admin 전용 팩 목록 조회 (대시보드용)
     * - 일반 사용자용 /api/training/pack과 동일한 로직이지만 admin 권한 필요
     * - admin 토큰 만료 시 조회 불가
     */
    @Operation(summary = "Get training pack list (Admin only)", description = "Admin-only pack list for dashboard")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/training/pack")
    @ResponseBody
    public ApiResponse<List<GetPackResponse>> getTrainingPackForAdmin(
            @Valid @ModelAttribute GetTrainingPackRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<GetPackResponse> packs = trainingService.getTrainingPackList(userDetails.getUser(), request);
        return ApiUtils.success(packs);
    }

    /**
     * Admin 전용 문제 목록 조회 (대시보드용)
     * - 일반 사용자용 /api/training/puzzle/{packId}와 동일한 로직이지만 admin 권한 필요
     * - admin 토큰 만료 시 조회 불가
     */
    @Operation(summary = "Get training puzzle list (Admin only)", description = "Admin-only puzzle list for dashboard")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/training/puzzle/{packId}")
    @ResponseBody
    public ApiResponse<List<GetTrainingPuzzleResponse>> getTrainingPuzzleForAdmin(
            @PathVariable("packId") Long packId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<GetTrainingPuzzleResponse> puzzles = trainingService.getTrainingPuzzleList(userDetails.getUser(), packId);
        return ApiUtils.success(puzzles);
    }

    public record AdminLoginResponse(
            String grantType,
            String accessToken,
            Instant accessTokenExpiredAt
    ) {}
}
