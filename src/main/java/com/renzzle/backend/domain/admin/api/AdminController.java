package com.renzzle.backend.domain.admin.api;

import com.renzzle.backend.domain.auth.api.request.LoginRequest;
import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.service.AccountService;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.puzzle.training.api.request.GetTrainingPackRequest;
import com.renzzle.backend.domain.puzzle.training.api.response.GetPackDetailForAdminResponse;
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
     * 어드민 팩 목록 조회 페이지 (첫 화면)
     */
    @Operation(summary = "Admin pack list page", description = "Pack list view - first screen after login")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/pack-list")
    public String packList(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(hidden = true) Model model
    ) {
        model.addAttribute("userEmail", userDetails.getUser().getEmail());
        return "admin/pack-list";
    }

    /**
     * 어드민 팩 생성 페이지
     */
    @Operation(summary = "Admin pack create page", description = "Pack creation form only")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/pack-create")
    public String packCreate(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return "admin/pack-create";
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
     * Admin 전용 팩 상세 조회 API
     */
    @Operation(summary = "Get pack detail (Admin only)", description = "Admin-only pack detail with translations")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/training/pack/{packId}")
    @ResponseBody
    public ApiResponse<GetPackDetailForAdminResponse> getPackDetailForAdmin(
            @PathVariable("packId") Long packId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        GetPackDetailForAdminResponse detail = trainingService.getPackDetailForAdmin(packId);
        return ApiUtils.success(detail);
    }

    /**
     * Admin 팩 세부 화면 (팩 이동 시)
     * - 상단: 제목, 작성자, 설명 (선택한 언어)
     * - 우상단: 문제 생성 버튼
     * - 본문: 팩 ID(읽기전용), 문제 순서
     */
    @Operation(summary = "Admin pack detail page", description = "Pack detail view with problem list")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/pack-detail")
    public String packDetail(
            @RequestParam("packId") Long packId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(hidden = true) Model model
    ) {
        model.addAttribute("packId", packId);
        model.addAttribute("userEmail", userDetails.getUser().getEmail());
        return "admin/pack-detail";
    }

    /**
     * Admin 문제 추가 화면
     * - 상단: 팩 ID, 제목, 작성자, 설명
     * - 본문: 보드 시각화, 보드상태, 정답, 깊이, 승리색상, 문제 추가 버튼
     */
    @Operation(summary = "Admin puzzle add page", description = "Add puzzle form for a pack")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/puzzle-add")
    public String puzzleAdd(
            @RequestParam("packId") Long packId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(hidden = true) Model model
    ) {
        model.addAttribute("packId", packId);
        model.addAttribute("userEmail", userDetails.getUser().getEmail());
        return "admin/puzzle-add";
    }

    /**
     * Admin 전용 문제 목록 조회 (대시보드용)
     * - 빈 팩도 빈 리스트로 반환 (pack-detail, puzzle-add에서 사용)
     */
    @Operation(summary = "Get training puzzle list (Admin only)", description = "Admin-only puzzle list for pack detail")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/training/puzzle/{packId}")
    @ResponseBody
    public ApiResponse<List<GetTrainingPuzzleResponse>> getTrainingPuzzleForAdmin(
            @PathVariable("packId") Long packId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<GetTrainingPuzzleResponse> puzzles = trainingService.getTrainingPuzzleListForAdmin(packId);
        return ApiUtils.success(puzzles);
    }

    public record AdminLoginResponse(
            String grantType,
            String accessToken,
            Instant accessTokenExpiredAt
    ) {}
}
