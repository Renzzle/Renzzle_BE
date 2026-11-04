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
import com.renzzle.backend.domain.payment.api.response.AppleTestNotificationResponse;
import com.renzzle.backend.domain.payment.api.response.GetAppleTestNotificationStatusResponse;
import com.renzzle.backend.domain.payment.api.response.GetGoogleVoidedPurchaseForAdminResponse;
import com.renzzle.backend.domain.payment.api.response.GetPurchaseForAdminResponse;
import com.renzzle.backend.domain.payment.service.PaymentAdminService;
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
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin login and admin-only APIs")
public class AdminController {

    private final AccountService accountService;
    private final AdminAccountService adminAccountService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final TrainingService trainingService;
    private final PackAccessGuard packAccessGuard;
    private final CommunityService communityService;
    private final NoticeService noticeService;
    private final PaymentAdminService paymentAdminService;
    private final Clock clock;

    @Operation(summary = "Admin login", description = "Admin-specific login that issues 12-hour access token.")
    @SecurityRequirement(name = "Authorization")
    @PostMapping("/login")
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

    private boolean isSecureRequest(HttpServletRequest request) {
        return request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    @Operation(summary = "Get training pack list (Admin only)", description = "Admin-only pack list for dashboard")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/training/pack")
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
    public ApiResponse<GetPackDetailForAdminResponse> getPackDetailForAdmin(
            @PathVariable Long packId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        packAccessGuard.checkReadPack(userDetails, packId);
        GetPackDetailForAdminResponse detail = trainingService.getPackDetailForAdmin(packId);
        return ApiUtils.success(detail);
    }

    @Operation(summary = "Get training puzzle list (Admin only)", description = "Admin-only puzzle list for pack detail")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/training/puzzle/{packId}")
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
    public ApiResponse<GetCommunityPuzzlePageForAdminResponse> getCommunityPuzzlesForAdmin(
            @Valid @ParameterObject @ModelAttribute GetCommunityPuzzlesForAdminRequest request
    ) {
        return ApiUtils.success(communityService.getCommunityPuzzlesForAdmin(request));
    }

    @Operation(summary = "Get community puzzle for admin management", description = "Admin-only lookup without increasing views")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/community/puzzle-manage/{puzzleId}")
    public ApiResponse<GetCommunityPuzzleForAdminResponse> getCommunityPuzzleForAdminManagement(
            @PathVariable Long puzzleId
    ) {
        GetCommunityPuzzleForAdminResponse puzzle = communityService.getCommunityPuzzleForAdminManagement(puzzleId);
        return ApiUtils.success(puzzle);
    }

    @Operation(summary = "Update community puzzle verification", description = "Admin-only isVerified update")
    @SecurityRequirement(name = "Authorization")
    @PatchMapping("/community/puzzle-manage/{puzzleId}/verification")
    public ApiResponse<GetCommunityPuzzleForAdminResponse> updateCommunityPuzzleVerification(
            @PathVariable Long puzzleId,
            @Valid @RequestBody UpdateCommunityPuzzleVerificationRequest request
    ) {
        GetCommunityPuzzleForAdminResponse puzzle = communityService.updateCommunityPuzzleVerificationForAdmin(
                puzzleId,
                request.isVerified(),
                request.answer()
        );
        return ApiUtils.success(puzzle);
    }

    @Operation(summary = "Recalculate puzzle ratings",
            description = "Admin-only; re-rates puzzles that have no rank results yet with the current formula")
    @SecurityRequirement(name = "Authorization")
    @PostMapping("/puzzle/rating/recalculate")
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
    public ApiResponse<RecalculateAnswerKeyResponse> recalculateAnswerKeys() {
        return ApiUtils.success(new RecalculateAnswerKeyResponse(
                trainingService.recalculateAnswerKeys(),
                communityService.recalculateAnswerKeys()
        ));
    }

    // ===== Notices (announcements, system info, personal notice) =====

    @Operation(summary = "Get announcements (Admin only)", description = "All announcements including expired ones, newest first. Optional lang filter.")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/notice/announcement")
    public ApiResponse<List<GetAnnouncementForAdminResponse>> getAnnouncementsForAdmin(
            @RequestParam(value = "lang", required = false) String lang
    ) {
        return ApiUtils.success(noticeService.getAnnouncementsForAdmin(lang));
    }

    @Operation(summary = "Create announcements (Admin only)", description = "Creates one announcement per language content with a shared expiration time and pin flag")
    @SecurityRequirement(name = "Authorization")
    @PostMapping("/notice/announcement")
    public ApiResponse<List<GetAnnouncementForAdminResponse>> createAnnouncements(
            @Valid @RequestBody CreateAnnouncementRequest request
    ) {
        return ApiUtils.success(noticeService.createAnnouncementsForAdmin(request));
    }

    @Operation(summary = "Update announcement (Admin only)", description = "Update title, context, expiration time, and pin flag of a single announcement")
    @SecurityRequirement(name = "Authorization")
    @PatchMapping("/notice/announcement/{announcementId}")
    public ApiResponse<GetAnnouncementForAdminResponse> updateAnnouncement(
            @PathVariable Long announcementId,
            @Valid @RequestBody UpdateAnnouncementRequest request
    ) {
        return ApiUtils.success(noticeService.updateAnnouncementForAdmin(announcementId, request));
    }

    @Operation(summary = "Delete announcement (Admin only)", description = "Permanently deletes a single announcement")
    @SecurityRequirement(name = "Authorization")
    @DeleteMapping("/notice/announcement/{announcementId}")
    public ApiResponse<Object> deleteAnnouncement(@PathVariable Long announcementId) {
        noticeService.deleteAnnouncementForAdmin(announcementId);
        return ApiUtils.success(null);
    }

    @Operation(summary = "Get system info (Admin only)", description = "Current required app version and system check flag")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/notice/system")
    public ApiResponse<GetSystemInfoForAdminResponse> getSystemInfoForAdmin() {
        return ApiUtils.success(noticeService.getSystemInfoForAdmin());
    }

    @Operation(summary = "Update system info (Admin only)", description = "Update required app version and system check flag")
    @SecurityRequirement(name = "Authorization")
    @PatchMapping("/notice/system")
    public ApiResponse<GetSystemInfoForAdminResponse> updateSystemInfo(
            @Valid @RequestBody UpdateSystemInfoRequest request
    ) {
        return ApiUtils.success(noticeService.updateSystemInfoForAdmin(request));
    }

    @Operation(summary = "Find notice recipient (Admin only)", description = "Look up a user by exact email or nickname")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/notice/user")
    public ApiResponse<GetNoticeRecipientResponse> findNoticeRecipient(
            @RequestParam("keyword") String keyword
    ) {
        return ApiUtils.success(noticeService.findNoticeRecipientForAdmin(keyword));
    }

    @Operation(summary = "Send personal notice (Admin only)", description = "Queue a one-time personal notice for a user")
    @SecurityRequirement(name = "Authorization")
    @PostMapping("/notice/personal")
    public ApiResponse<GetNoticeRecipientResponse> sendPersonalNotice(
            @Valid @RequestBody SendPersonalNoticeRequest request
    ) {
        return ApiUtils.success(noticeService.sendPersonalNoticeForAdmin(request));
    }

    // ===== Payments (refund lookups for CS, App Store notification check) =====

    @Operation(summary = "Search purchases (Admin only)",
            description = "By Google order ID, App Store transaction ID, purchase token, or exact user email or nickname. "
                    + "Each purchase says whether it is refundable under the refund policy")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/payment/purchases")
    public ApiResponse<List<GetPurchaseForAdminResponse>> searchPurchasesForAdmin(
            @RequestParam("keyword") String keyword
    ) {
        return ApiUtils.success(paymentAdminService.searchPurchasesForAdmin(keyword));
    }

    @Operation(summary = "Google Play refunds of the last 30 days (Admin only)",
            description = "Read live from Google, with the purchase each one matches and what it takes back")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/payment/google-voided")
    public ApiResponse<List<GetGoogleVoidedPurchaseForAdminResponse>> getGoogleVoidedPurchasesForAdmin() {
        return ApiUtils.success(paymentAdminService.getGoogleVoidedPurchasesForAdmin());
    }

    @Operation(summary = "Request an App Store test notification (Admin only)",
            description = "Apple sends a TEST notification to the URL registered for the environment (Production or Sandbox)")
    @SecurityRequirement(name = "Authorization")
    @PostMapping("/payment/apple/test-notification")
    public ApiResponse<AppleTestNotificationResponse> requestAppleTestNotification(
            @RequestParam(value = "environment", defaultValue = "Production") String environment
    ) {
        return ApiUtils.success(paymentAdminService.requestAppleTestNotificationForAdmin(environment));
    }

    @Operation(summary = "App Store test notification result (Admin only)",
            description = "Whether Apple could deliver the test notification, and why not")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/payment/apple/test-notification/{testNotificationToken}")
    public ApiResponse<GetAppleTestNotificationStatusResponse> getAppleTestNotificationStatus(
            @PathVariable String testNotificationToken,
            @RequestParam(value = "environment", defaultValue = "Production") String environment
    ) {
        return ApiUtils.success(
                paymentAdminService.getAppleTestNotificationStatusForAdmin(environment, testNotificationToken));
    }

    // ===== Admin accounts =====

    @Operation(summary = "Get admin accounts (Admin only)", description = "Every admin with their permissions")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/admin-account")
    public ApiResponse<List<GetAdminAccountResponse>> getAdminAccounts(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ApiUtils.success(adminAccountService.getAdminAccounts(userDetails.getUser()));
    }

    @Operation(summary = "Add admin account (Admin only)", description = "Makes an existing app user an admin with the given permissions")
    @SecurityRequirement(name = "Authorization")
    @PostMapping("/admin-account")
    public ApiResponse<GetAdminAccountResponse> addAdminAccount(
            @Valid @RequestBody AddAdminAccountRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ApiUtils.success(adminAccountService.addAdminAccount(request, userDetails.getUser()));
    }

    @Operation(summary = "Update admin permissions (Admin only)", description = "Replaces the admin's permissions; your own operation permission cannot be removed")
    @SecurityRequirement(name = "Authorization")
    @PatchMapping("/admin-account/{adminId}")
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
