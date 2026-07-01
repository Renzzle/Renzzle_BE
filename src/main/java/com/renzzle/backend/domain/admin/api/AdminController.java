package com.renzzle.backend.domain.admin.api;

import com.renzzle.backend.domain.auth.api.request.LoginRequest;
import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.service.AccountService;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.puzzle.training.api.response.GetPackDetailForAdminResponse;
import com.renzzle.backend.domain.puzzle.training.api.response.GetPackResponse;
import com.renzzle.backend.domain.puzzle.training.api.response.GetTrainingPuzzleForAdminResponse;
import com.renzzle.backend.domain.puzzle.community.service.CommunityService;
import com.renzzle.backend.domain.puzzle.training.service.TrainingService;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.LangCode;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin login and dashboard page")
public class AdminController {

    private static final String USER_EMAIL = "userEmail";
    private static final String LANG_CODE_NAMES = "langCodeNames";
    private static final String PACK_ID = "packId";

    private final AccountService accountService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final TrainingService trainingService;
    private final CommunityService communityService;
    private final Clock clock;

    /**
     * Admin login page
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
     * Admin-only login API (12-hour token)
     */
    @Operation(summary = "Admin login", description = "Admin-specific login that issues 12-hour access token.")
    @SecurityRequirement(name = "Authorization")
    @PostMapping("/login")
    @ResponseBody
    public ApiResponse<AdminLoginResponse> adminLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        // Handle normal login
        var loginResponse = accountService.login(request);

        // Verify admin
        long userId = jwtProvider.getUserId(loginResponse.accessToken());
        Optional<UserEntity> user = userRepository.findById(userId);
        if (user.isEmpty() || !adminRepository.existsByUser(user.get())) {
            throw new CustomException(ErrorCode.ADMIN_ACCESS_DENIED);
        }

        // Reissue admin-only token (12 hours)
        String adminAccessToken = jwtProvider.createAdminAccessToken(userId);
        Instant expiredAt = clock.instant().plus(12, ChronoUnit.HOURS);
        ResponseCookie cookie = ResponseCookie.from("admin_accessToken", adminAccessToken)
                .httpOnly(true)
                .secure(isSecureRequest(servletRequest))
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofHours(12))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiUtils.success(new AdminLoginResponse(
                "Bearer",
                adminAccessToken,
                expiredAt
        ));
    }

    /**
     * Admin dashboard
     */
    @Operation(summary = "Admin dashboard", description = "Admin dashboard page (requires admin token from /admin/login)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard HTML",
                    content = @Content(mediaType = MediaType.TEXT_HTML_VALUE))
    })
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/dashboard")
    public String dashboard(
            ) {
        return "redirect:/admin/pack-list";
    }

    /**
     * Admin pack list page (first screen)
     */
    @Operation(summary = "Admin pack list page", description = "Pack list view - first screen after login")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/pack-list")
    public String packList(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(hidden = true) Model model
    ) {
        model.addAttribute(USER_EMAIL, userDetails.getUser().getEmail());
        model.addAttribute(LANG_CODE_NAMES, LangCode.LangCodeName.values());
        return "admin/pack-list";
    }

    /**
     * Admin pack create page
     */
    @Operation(summary = "Admin pack create page", description = "Pack creation form only")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/pack-create")
    public String packCreate(
            @RequestParam(value = PACK_ID, required = false) Long packId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(hidden = true) Model model
    ) {
        if (packId == null) {
            return "redirect:/admin/pack-list";
        }
        model.addAttribute(USER_EMAIL, userDetails.getUser().getEmail());
        model.addAttribute(LANG_CODE_NAMES, LangCode.LangCodeName.values());
        return "admin/pack-create";
    }

    /**
     * Admin logout
     * - Delete admin_accessToken stored in the browser cookie
     * - Redirect to the login page (/admin)
     */
    @Operation(summary = "Admin logout", description = "Clear admin_accessToken cookie and redirect to login page.")
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("admin_accessToken", "")
                .httpOnly(true)
                .secure(isSecureRequest(request))
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return "redirect:/admin";
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        return request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    /**
     * Admin-only pack list (for the dashboard)
     * - Same logic as the user-facing /api/training/pack but requires admin privileges
     * - Not accessible once the admin token has expired
     */
    @Operation(summary = "Get training pack list (Admin only)", description = "Admin-only pack list for dashboard")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/training/pack")
    @ResponseBody
    public ApiResponse<List<GetPackResponse>> getTrainingPackForAdmin(
            @RequestParam(defaultValue = "LOW") String difficulty,
            @RequestParam(defaultValue = "EN") String lang,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<GetPackResponse> packs = trainingService.getTrainingPackListForAdmin(userDetails.getUser(), difficulty, lang);
        return ApiUtils.success(packs);
    }

    /**
     * Admin-only pack detail API
     */
    @Operation(summary = "Get pack detail (Admin only)", description = "Admin-only pack detail with translations")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/training/pack/{packId}")
    @ResponseBody
    public ApiResponse<GetPackDetailForAdminResponse> getPackDetailForAdmin(
            @PathVariable Long packId
    ) {
        GetPackDetailForAdminResponse detail = trainingService.getPackDetailForAdmin(packId);
        return ApiUtils.success(detail);
    }

    /**
     * Admin pack detail screen (when navigating to a pack)
     * - Top: title, author, description (selected language)
     * - Top-right: create problem button
     * - Body: pack ID (read-only), problem order
     */
    @Operation(summary = "Admin pack detail page", description = "Pack detail view with problem list")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/pack-detail")
    public String packDetail(
            @RequestParam(PACK_ID) Long packId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(hidden = true) Model model
    ) {
        model.addAttribute(PACK_ID, packId);
        model.addAttribute(USER_EMAIL, userDetails.getUser().getEmail());
        model.addAttribute(LANG_CODE_NAMES, LangCode.LangCodeName.values());
        return "admin/pack-detail";
    }

    /**
     * Admin problem add screen
     * - Top: pack ID, title, author, description
     * - Body: board visualization, board status, answer, depth, win color, add problem button
     */
    @Operation(summary = "Admin puzzle add page", description = "Add puzzle form for a pack")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/puzzle-add")
    public String puzzleAdd(
            @RequestParam(PACK_ID) Long packId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(hidden = true) Model model
    ) {
        model.addAttribute(PACK_ID, packId);
        model.addAttribute(USER_EMAIL, userDetails.getUser().getEmail());
        return "admin/puzzle-add";
    }

    @Operation(summary = "Admin puzzle edit page", description = "Edit puzzle form - same layout as puzzle-add, data loading deferred")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/puzzle-edit")
    public String puzzleEdit(
            @RequestParam(PACK_ID) Long packId,
            @RequestParam("puzzleId") Long puzzleId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(hidden = true) Model model
    ) {
        model.addAttribute(PACK_ID, packId);
        model.addAttribute("puzzleId", puzzleId);
        model.addAttribute(USER_EMAIL, userDetails.getUser().getEmail());
        return "admin/puzzle-edit";
    }

    /**
     * Admin-only problem list (for the dashboard)
     * - Returns an empty list even for empty packs (used by pack-detail, puzzle-add)
     */
    @Operation(summary = "Get training puzzle list (Admin only)", description = "Admin-only puzzle list for pack detail")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/training/puzzle/{packId}")
    @ResponseBody
    public ApiResponse<List<GetTrainingPuzzleForAdminResponse>> getTrainingPuzzleForAdmin(
            @PathVariable Long packId
    ) {
        List<GetTrainingPuzzleForAdminResponse> puzzles = trainingService.getTrainingPuzzleListForAdmin(packId);
        return ApiUtils.success(puzzles);
    }

    @Operation(summary = "Get single puzzle for admin edit", description = "Admin-only single puzzle detail")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/training/puzzle-detail/{puzzleId}")
    @ResponseBody
    public ApiResponse<GetTrainingPuzzleForAdminResponse> getTrainingPuzzleByIdForAdmin(
            @PathVariable Long puzzleId
    ) {
        GetTrainingPuzzleForAdminResponse puzzle = trainingService.getTrainingPuzzleByIdForAdmin(puzzleId);
        return ApiUtils.success(puzzle);
    }

    @Operation(summary = "Community puzzle detail for admin cache", description = "Admin-only lookup that includes answer without increasing views")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/community/puzzle-detail/{puzzleId}")
    @ResponseBody
    public ApiResponse<GetTrainingPuzzleForAdminResponse> getCommunityPuzzleDetailForAdmin(
            @PathVariable Long puzzleId
    ) {
        GetTrainingPuzzleForAdminResponse puzzle = communityService.getCommunityPuzzleForAdminDetail(puzzleId);
        return ApiUtils.success(puzzle);
    }

    public record AdminLoginResponse(
            String grantType,
            String accessToken,
            Instant accessTokenExpiredAt
    ) {}
}
