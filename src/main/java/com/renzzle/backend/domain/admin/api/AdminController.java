package com.renzzle.backend.domain.admin.api;

import com.renzzle.backend.domain.admin.api.request.AddAdminAccountRequest;
import com.renzzle.backend.domain.admin.api.request.UpdateAdminPermissionRequest;
import com.renzzle.backend.domain.admin.api.response.GetAdminAccountResponse;
import com.renzzle.backend.domain.admin.api.response.RecalculateAnswerKeyResponse;
import com.renzzle.backend.domain.admin.api.response.RecalculateBoardKeyResponse;
import com.renzzle.backend.domain.admin.api.response.RecalculatePuzzleRatingResponse;
import com.renzzle.backend.domain.auth.api.request.LoginRequest;
import com.renzzle.backend.domain.admin.service.AdminAccountService;
import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.domain.Admin;
import com.renzzle.backend.domain.auth.domain.AdminPermission;
import com.renzzle.backend.domain.auth.service.AccountService;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.notice.api.request.CreateAnnouncementRequest;
import com.renzzle.backend.domain.notice.api.request.SendPersonalNoticeRequest;
import com.renzzle.backend.domain.notice.api.request.UpdateAnnouncementRequest;
import com.renzzle.backend.domain.notice.api.request.UpdateSystemInfoRequest;
import com.renzzle.backend.domain.notice.api.response.GetAnnouncementForAdminResponse;
import com.renzzle.backend.domain.notice.api.response.GetNoticeRecipientResponse;
import com.renzzle.backend.domain.notice.api.response.GetSystemInfoForAdminResponse;
import com.renzzle.backend.domain.notice.service.NoticeService;
import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzlesForAdminRequest;
import com.renzzle.backend.domain.puzzle.community.api.request.UpdateCommunityPuzzleVerificationRequest;
import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzleForAdminResponse;
import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzlePageForAdminResponse;
import com.renzzle.backend.domain.puzzle.training.api.response.GetPackDetailForAdminResponse;
import com.renzzle.backend.domain.puzzle.training.api.response.GetPackForAdminResponse;
import com.renzzle.backend.domain.puzzle.training.api.response.GetTrainingPuzzleForAdminResponse;
import com.renzzle.backend.domain.puzzle.community.service.CommunityService;
import com.renzzle.backend.domain.puzzle.training.service.PackAccessGuard;
import com.renzzle.backend.domain.puzzle.training.service.TrainingService;
import com.renzzle.backend.domain.user.dao.UserRepository;
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
import org.springdoc.core.annotations.ParameterObject;
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
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin login and dashboard page")
public class AdminController {

    private static final String USER_EMAIL = "userEmail";
    private static final String LANG_CODE_NAMES = "langCodeNames";
    private static final String PACK_ID = "packId";
    private static final String FORBIDDEN_REDIRECT = "redirect:/admin/forbidden";
    // Iterates in permission order, so the first permitted tab is the landing page
    private static final Map<AdminPermission, String> HOME_PAGES = new EnumMap<>(Map.of(
            AdminPermission.PACK_MANAGE, "/admin/pack-list",
            AdminPermission.COMMUNITY_MANAGE, "/admin/community-puzzles",
            AdminPermission.AI_RESPONSE_MANAGE, "/admin/puzzle-cache",
            AdminPermission.OPERATION_MANAGE, "/admin/notices"
    ));

    private final AccountService accountService;
    private final AdminAccountService adminAccountService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final TrainingService trainingService;
    private final PackAccessGuard packAccessGuard;
    private final CommunityService communityService;
    private final NoticeService noticeService;
    private final Clock clock;

    @Operation(summary = "Admin login page", description = "Returns admin login page.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login page HTML",
                    content = @Content(mediaType = MediaType.TEXT_HTML_VALUE))
    })
    @GetMapping
    public String loginPage() {
        return "admin/login";
    }

    @Operation(summary = "Admin login", description = "Admin-specific login that issues 12-hour access token.")
    @SecurityRequirement(name = "Authorization")
    @PostMapping("/login")
    @ResponseBody
    public ApiResponse<AdminLoginResponse> adminLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        var loginResponse = accountService.login(request);

        long userId = jwtProvider.parseAccessToken(loginResponse.accessToken()).userId();
        Admin admin = userRepository.findById(userId)
                .flatMap(adminRepository::findByUser)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_ACCESS_DENIED));
        if (admin.getPermissions().isEmpty()) {
            throw new CustomException(ErrorCode.ADMIN_PERMISSION_DENIED);
        }

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

    @Operation(summary = "Admin dashboard", description = "Admin dashboard page (requires admin token from /admin/login)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard HTML",
                    content = @Content(mediaType = MediaType.TEXT_HTML_VALUE))
    })
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/dashboard")
    public String dashboard(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Set<AdminPermission> permissions = userDetails.getAdminPermissions();
        return HOME_PAGES.entrySet().stream()
                .filter(page -> permissions.contains(page.getKey()))
                .map(page -> "redirect:" + page.getValue())
                .findFirst()
                .orElse(FORBIDDEN_REDIRECT);
    }

    @Operation(summary = "Admin forbidden page", description = "Shown when an admin opens a menu without its permission")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/forbidden")
    public String forbidden(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(hidden = true) Model model
    ) {
        model.addAttribute(USER_EMAIL, userDetails.getUser().getEmail());
        return "admin/forbidden";
    }

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
        if (packAccessGuard.isForeignPack(userDetails, packId)) {
            return FORBIDDEN_REDIRECT;
        }
        model.addAttribute(USER_EMAIL, userDetails.getUser().getEmail());
        model.addAttribute(LANG_CODE_NAMES, LangCode.LangCodeName.values());
        return "admin/pack-create";
    }

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

    @Operation(summary = "Get training pack list (Admin only)", description = "Admin-only pack list for dashboard")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/training/pack")
    @ResponseBody
    public ApiResponse<List<GetPackForAdminResponse>> getTrainingPackForAdmin(
            @RequestParam(defaultValue = "LOW") String difficulty,
            @RequestParam(defaultValue = "EN") String lang,
            @Parameter(description = "Only packs the admin may edit (the pack pages); AI response pages read every pack")
            @RequestParam(defaultValue = "false") boolean manageable,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<GetPackForAdminResponse> packs = trainingService.getTrainingPackListForAdmin(
                difficulty, lang, packAccessGuard.listCreatorFilter(userDetails, manageable));
        return ApiUtils.success(packs);
    }

    @Operation(summary = "Get pack detail (Admin only)", description = "Admin-only pack detail with translations")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/training/pack/{packId}")
    @ResponseBody
    public ApiResponse<GetPackDetailForAdminResponse> getPackDetailForAdmin(
            @PathVariable Long packId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        packAccessGuard.checkReadPack(userDetails, packId);
        GetPackDetailForAdminResponse detail = trainingService.getPackDetailForAdmin(packId);
        return ApiUtils.success(detail);
    }

    @Operation(summary = "Admin pack detail page", description = "Pack detail view with problem list")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/pack-detail")
    public String packDetail(
            @RequestParam(PACK_ID) Long packId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(hidden = true) Model model
    ) {
        if (packAccessGuard.isForeignPack(userDetails, packId)) {
            return FORBIDDEN_REDIRECT;
        }
        model.addAttribute(PACK_ID, packId);
        model.addAttribute(USER_EMAIL, userDetails.getUser().getEmail());
        model.addAttribute(LANG_CODE_NAMES, LangCode.LangCodeName.values());
        return "admin/pack-detail";
    }

    @Operation(summary = "Admin puzzle add page", description = "Add puzzle form for a pack")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/puzzle-add")
    public String puzzleAdd(
            @RequestParam(PACK_ID) Long packId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(hidden = true) Model model
    ) {
        if (packAccessGuard.isForeignPack(userDetails, packId)) {
            return FORBIDDEN_REDIRECT;
        }
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
        if (packAccessGuard.isForeignPack(userDetails, packId)) {
            return FORBIDDEN_REDIRECT;
        }
        model.addAttribute(PACK_ID, packId);
        model.addAttribute("puzzleId", puzzleId);
        model.addAttribute(USER_EMAIL, userDetails.getUser().getEmail());
        return "admin/puzzle-edit";
    }

    @Operation(summary = "Admin community puzzle management page", description = "Search and manage community puzzle verification")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/community-puzzles")
    public String communityPuzzles(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(hidden = true) Model model
    ) {
        model.addAttribute(USER_EMAIL, userDetails.getUser().getEmail());
        return "admin/community-puzzles";
    }

    @Operation(summary = "Get training puzzle list (Admin only)", description = "Admin-only puzzle list for pack detail")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/training/puzzle/{packId}")
    @ResponseBody
    public ApiResponse<List<GetTrainingPuzzleForAdminResponse>> getTrainingPuzzleForAdmin(
            @PathVariable Long packId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        packAccessGuard.checkReadPack(userDetails, packId);
        List<GetTrainingPuzzleForAdminResponse> puzzles = trainingService.getTrainingPuzzleListForAdmin(packId);
        return ApiUtils.success(puzzles);
    }

    @Operation(summary = "Get single puzzle for admin edit", description = "Admin-only single puzzle detail")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/training/puzzle-detail/{puzzleId}")
    @ResponseBody
    public ApiResponse<GetTrainingPuzzleForAdminResponse> getTrainingPuzzleByIdForAdmin(
            @PathVariable Long puzzleId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        packAccessGuard.checkReadPuzzle(userDetails, puzzleId);
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

    @Operation(summary = "Get community puzzles for admin review",
            description = "Admin-only, newest first; verified=false lists puzzles waiting for review and no verified lists them all")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/community/puzzle-manage")
    @ResponseBody
    public ApiResponse<GetCommunityPuzzlePageForAdminResponse> getCommunityPuzzlesForAdmin(
            @Valid @ParameterObject @ModelAttribute GetCommunityPuzzlesForAdminRequest request
    ) {
        return ApiUtils.success(communityService.getCommunityPuzzlesForAdmin(request));
    }

    @Operation(summary = "Get community puzzle for admin management", description = "Admin-only lookup without increasing views")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/community/puzzle-manage/{puzzleId}")
    @ResponseBody
    public ApiResponse<GetCommunityPuzzleForAdminResponse> getCommunityPuzzleForAdminManagement(
            @PathVariable Long puzzleId
    ) {
        GetCommunityPuzzleForAdminResponse puzzle = communityService.getCommunityPuzzleForAdminManagement(puzzleId);
        return ApiUtils.success(puzzle);
    }

    @Operation(summary = "Update community puzzle verification", description = "Admin-only isVerified update")
    @SecurityRequirement(name = "Authorization")
    @PatchMapping("/community/puzzle-manage/{puzzleId}/verification")
    @ResponseBody
    public ApiResponse<GetCommunityPuzzleForAdminResponse> updateCommunityPuzzleVerification(
            @PathVariable Long puzzleId,
            @Valid @RequestBody UpdateCommunityPuzzleVerificationRequest request
    ) {
        GetCommunityPuzzleForAdminResponse puzzle = communityService.updateCommunityPuzzleVerificationForAdmin(
                puzzleId,
                request.isVerified()
        );
        return ApiUtils.success(puzzle);
    }

    @Operation(summary = "Recalculate puzzle ratings",
            description = "Admin-only; re-rates puzzles that have no rank results yet with the current formula")
    @SecurityRequirement(name = "Authorization")
    @PostMapping("/puzzle/rating/recalculate")
    @ResponseBody
    public ApiResponse<RecalculatePuzzleRatingResponse> recalculatePuzzleRatings() {
        return ApiUtils.success(new RecalculatePuzzleRatingResponse(
                trainingService.recalculateUnrankedPuzzleRatings(),
                communityService.recalculateUnrankedPuzzleRatings()
        ));
    }

    @Operation(summary = "Recalculate puzzle board keys",
            description = "Admin-only; recomputes every puzzle's duplicate-detection key with the current formula. "
                    + "Puzzles that turn out to be the same position keep their keys and are listed under duplicates")
    @SecurityRequirement(name = "Authorization")
    @PostMapping("/puzzle/board-key/recalculate")
    @ResponseBody
    public ApiResponse<RecalculateBoardKeyResponse> recalculateBoardKeys() {
        return ApiUtils.success(new RecalculateBoardKeyResponse(
                trainingService.recalculateBoardKeys(),
                communityService.recalculateBoardKeys()
        ));
    }

    @Operation(summary = "Recalculate puzzle answer keys",
            description = "Admin-only; recomputes the key that finds uploads copying a puzzle with a few stones changed. "
                    + "Puzzles whose board or answer cannot be parsed are listed under invalidIds")
    @SecurityRequirement(name = "Authorization")
    @PostMapping("/puzzle/answer-key/recalculate")
    @ResponseBody
    public ApiResponse<RecalculateAnswerKeyResponse> recalculateAnswerKeys() {
        return ApiUtils.success(new RecalculateAnswerKeyResponse(
                trainingService.recalculateAnswerKeys(),
                communityService.recalculateAnswerKeys()
        ));
    }

    // ===== Notices (announcements, system info, personal notice) =====

    @Operation(summary = "Admin notice management page", description = "Manage announcements, system info, and personal notices")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/notices")
    public String notices(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(hidden = true) Model model
    ) {
        model.addAttribute(USER_EMAIL, userDetails.getUser().getEmail());
        model.addAttribute(LANG_CODE_NAMES, LangCode.LangCodeName.values());
        return "admin/notices";
    }

    @Operation(summary = "Get announcements (Admin only)", description = "All announcements including expired ones, newest first. Optional lang filter.")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/notice/announcement")
    @ResponseBody
    public ApiResponse<List<GetAnnouncementForAdminResponse>> getAnnouncementsForAdmin(
            @RequestParam(value = "lang", required = false) String lang
    ) {
        return ApiUtils.success(noticeService.getAnnouncementsForAdmin(lang));
    }

    @Operation(summary = "Create announcements (Admin only)", description = "Creates one announcement per language content with a shared expiration time")
    @SecurityRequirement(name = "Authorization")
    @PostMapping("/notice/announcement")
    @ResponseBody
    public ApiResponse<List<GetAnnouncementForAdminResponse>> createAnnouncements(
            @Valid @RequestBody CreateAnnouncementRequest request
    ) {
        return ApiUtils.success(noticeService.createAnnouncementsForAdmin(request));
    }

    @Operation(summary = "Update announcement (Admin only)", description = "Update title, context, and expiration time of a single announcement")
    @SecurityRequirement(name = "Authorization")
    @PatchMapping("/notice/announcement/{announcementId}")
    @ResponseBody
    public ApiResponse<GetAnnouncementForAdminResponse> updateAnnouncement(
            @PathVariable Long announcementId,
            @Valid @RequestBody UpdateAnnouncementRequest request
    ) {
        return ApiUtils.success(noticeService.updateAnnouncementForAdmin(announcementId, request));
    }

    @Operation(summary = "Delete announcement (Admin only)", description = "Permanently deletes a single announcement")
    @SecurityRequirement(name = "Authorization")
    @DeleteMapping("/notice/announcement/{announcementId}")
    @ResponseBody
    public ApiResponse<Object> deleteAnnouncement(@PathVariable Long announcementId) {
        noticeService.deleteAnnouncementForAdmin(announcementId);
        return ApiUtils.success(null);
    }

    @Operation(summary = "Get system info (Admin only)", description = "Current required app version and system check flag")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/notice/system")
    @ResponseBody
    public ApiResponse<GetSystemInfoForAdminResponse> getSystemInfoForAdmin() {
        return ApiUtils.success(noticeService.getSystemInfoForAdmin());
    }

    @Operation(summary = "Update system info (Admin only)", description = "Update required app version and system check flag")
    @SecurityRequirement(name = "Authorization")
    @PatchMapping("/notice/system")
    @ResponseBody
    public ApiResponse<GetSystemInfoForAdminResponse> updateSystemInfo(
            @Valid @RequestBody UpdateSystemInfoRequest request
    ) {
        return ApiUtils.success(noticeService.updateSystemInfoForAdmin(request));
    }

    @Operation(summary = "Find notice recipient (Admin only)", description = "Look up a user by exact email or nickname")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/notice/user")
    @ResponseBody
    public ApiResponse<GetNoticeRecipientResponse> findNoticeRecipient(
            @RequestParam("keyword") String keyword
    ) {
        return ApiUtils.success(noticeService.findNoticeRecipientForAdmin(keyword));
    }

    @Operation(summary = "Send personal notice (Admin only)", description = "Queue a one-time personal notice for a user")
    @SecurityRequirement(name = "Authorization")
    @PostMapping("/notice/personal")
    @ResponseBody
    public ApiResponse<GetNoticeRecipientResponse> sendPersonalNotice(
            @Valid @RequestBody SendPersonalNoticeRequest request
    ) {
        return ApiUtils.success(noticeService.sendPersonalNoticeForAdmin(request));
    }

    // ===== Admin accounts =====

    @Operation(summary = "Admin account management page", description = "Grant admin access to app users and set their permissions")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/admin-accounts")
    public String adminAccounts(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Parameter(hidden = true) Model model
    ) {
        model.addAttribute(USER_EMAIL, userDetails.getUser().getEmail());
        model.addAttribute("permissionNames", Arrays.stream(AdminPermission.values()).map(Enum::name).toList());
        return "admin/admin-accounts";
    }

    @Operation(summary = "Get admin accounts (Admin only)", description = "Every admin with their permissions")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/admin-account")
    @ResponseBody
    public ApiResponse<List<GetAdminAccountResponse>> getAdminAccounts(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ApiUtils.success(adminAccountService.getAdminAccounts(userDetails.getUser()));
    }

    @Operation(summary = "Add admin account (Admin only)", description = "Makes an existing app user an admin with the given permissions")
    @SecurityRequirement(name = "Authorization")
    @PostMapping("/admin-account")
    @ResponseBody
    public ApiResponse<GetAdminAccountResponse> addAdminAccount(
            @Valid @RequestBody AddAdminAccountRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ApiUtils.success(adminAccountService.addAdminAccount(request, userDetails.getUser()));
    }

    @Operation(summary = "Update admin permissions (Admin only)", description = "Replaces the admin's permissions; your own operation permission cannot be removed")
    @SecurityRequirement(name = "Authorization")
    @PatchMapping("/admin-account/{adminId}")
    @ResponseBody
    public ApiResponse<GetAdminAccountResponse> updateAdminPermissions(
            @PathVariable Long adminId,
            @Valid @RequestBody UpdateAdminPermissionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ApiUtils.success(adminAccountService.updatePermissions(adminId, request.permissions(), userDetails.getUser()));
    }

    @Operation(summary = "Remove admin account (Admin only)", description = "Revokes admin access; the app account itself stays")
    @SecurityRequirement(name = "Authorization")
    @DeleteMapping("/admin-account/{adminId}")
    @ResponseBody
    public ApiResponse<Object> removeAdminAccount(
            @PathVariable Long adminId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        adminAccountService.removeAdminAccount(adminId, userDetails.getUser());
        return ApiUtils.success(null);
    }

    public record AdminLoginResponse(
            String grantType,
            String accessToken,
            Instant accessTokenExpiredAt
    ) {}
}
