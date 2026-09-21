package com.renzzle.backend.domain.notice.service;

import com.renzzle.backend.domain.notice.api.request.AnnouncementContentRequest;
import com.renzzle.backend.domain.notice.api.request.CreateAnnouncementRequest;
import com.renzzle.backend.domain.notice.api.request.GetPersonalNoticeRequest;
import com.renzzle.backend.domain.notice.api.request.GetPublicNoticeRequest;
import com.renzzle.backend.domain.notice.api.request.SendPersonalNoticeRequest;
import com.renzzle.backend.domain.notice.api.request.UpdateAnnouncementRequest;
import com.renzzle.backend.domain.notice.api.request.UpdateSystemInfoRequest;
import com.renzzle.backend.domain.notice.api.response.GetAnnouncementForAdminResponse;
import com.renzzle.backend.domain.notice.api.response.GetNoticeRecipientResponse;
import com.renzzle.backend.domain.notice.api.response.GetPersonalNoticeResponse;
import com.renzzle.backend.domain.notice.api.response.GetPublicNoticeResponse;
import com.renzzle.backend.domain.notice.api.response.GetSystemInfoForAdminResponse;
import com.renzzle.backend.domain.notice.api.response.NoticeContext;
import com.renzzle.backend.domain.notice.dao.AnnouncementRepository;
import com.renzzle.backend.domain.notice.dao.NoticeRepository;
import com.renzzle.backend.domain.notice.dao.SystemInfoRepository;
import com.renzzle.backend.domain.notice.domain.Announcement;
import com.renzzle.backend.domain.notice.domain.Notice;
import com.renzzle.backend.domain.notice.domain.SystemInfo;
import com.renzzle.backend.domain.notice.util.NoticeTextBuilderUtil;
import com.renzzle.backend.domain.user.dao.UserRepository;

import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final Clock clock;
    private final NoticeRepository noticeRepository;
    private final AnnouncementRepository announcementRepository;
    private final SystemInfoRepository systemInfoRepository;
    private final UserRepository userRepository;

    @Transactional
    public GetPersonalNoticeResponse getPersonalNotice(GetPersonalNoticeRequest request, UserEntity user) {
        SystemInfo systemInfo = systemInfoRepository.getSystemInfo().orElseThrow(
                () -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR)
        );

        // System Check
        if (systemInfo.isSystemCheck()) {
            return GetPersonalNoticeResponse.builder()
                    .description("system-check")
                    .build();
        }
        // App version check
        if (!request.version().trim().equals(systemInfo.getVersion())) {
            return GetPersonalNoticeResponse.builder()
                    .description("update")
                    .version(systemInfo.getVersion())
                    .build();
        }

        // Get notices context
        ArrayList<NoticeContext> contexts = new ArrayList<>();

        // 1. Personal message
        List<Notice> notices = noticeRepository.findAllByUser(user);
        for (Notice notice : notices) {
            contexts.add(NoticeContext.builder()
                    .context(notice.getContext())
                    .build()
            );
        }
        noticeRepository.deleteAllByUser(user);

        // 2. Attendance price
        if (Boolean.TRUE.equals(userRepository.isLastAccessBeforeToday(user.getId()))) {
            int price = 200;
            userRepository.addUserCurrency(user.getId(), price);
            contexts.add(NoticeContext.builder()
                    .context(NoticeTextBuilderUtil.buildAttendanceMessage(LangCode.getLangCode(request.langCode()), price))
                    .build()
            );
        }
        userRepository.updateLastAccessedAt(user.getId(), clock.instant());

        return GetPersonalNoticeResponse.builder()
                .description("context")
                .notice(contexts)
                .build();
    }

    @Transactional(readOnly = true)
    public List<GetPublicNoticeResponse> getPublicNotice(GetPublicNoticeRequest request) {
        List<Announcement> activeAnnouncements = announcementRepository.findAllByLangCodeAndExpiredAtAfter(LangCode.getLangCode(request.langCode()), clock.instant());

        ArrayList<GetPublicNoticeResponse> response = new ArrayList<>();
        for (Announcement announcement : activeAnnouncements) {
            response.add(GetPublicNoticeResponse.builder()
                    .title(announcement.getTitle())
                    .context(announcement.getContext())
                    .createdAt(announcement.getCreatedAt().toString())
                    .expiredAt(announcement.getExpiredAt().toString())
                    .build()
            );
        }

        return response;
    }

    // ===== Admin: announcements =====

    @Transactional(readOnly = true)
    public List<GetAnnouncementForAdminResponse> getAnnouncementsForAdmin(String langCode) {
        List<Announcement> announcements = (langCode == null || langCode.isBlank())
                ? announcementRepository.findAllByOrderByCreatedAtDescIdDesc()
                : announcementRepository.findAllByLangCodeOrderByCreatedAtDescIdDesc(LangCode.getLangCode(langCode));

        Instant now = clock.instant();
        List<GetAnnouncementForAdminResponse> response = new ArrayList<>();
        for (Announcement announcement : announcements) {
            response.add(toAdminAnnouncementResponse(announcement, now));
        }
        return response;
    }

    /**
     * Creates one announcement row per language content in the request.
     * The same expiration time is applied to every row so translations retire together.
     */
    @Transactional
    public List<GetAnnouncementForAdminResponse> createAnnouncementsForAdmin(CreateAnnouncementRequest request) {
        Set<String> seenLangCodes = new HashSet<>();
        for (AnnouncementContentRequest content : request.contents()) {
            if (!seenLangCodes.add(content.langCode().toUpperCase())) {
                throw new CustomException("Duplicate language content: " + content.langCode(), ErrorCode.VALIDATION_ERROR);
            }
        }

        Instant now = clock.instant();
        List<GetAnnouncementForAdminResponse> response = new ArrayList<>();
        for (AnnouncementContentRequest content : request.contents()) {
            Announcement announcement = announcementRepository.save(Announcement.builder()
                    .langCode(LangCode.getLangCode(content.langCode()))
                    .title(content.title().trim())
                    .context(content.context().trim())
                    .expiredAt(request.expiredAt())
                    .build());
            response.add(toAdminAnnouncementResponse(announcement, now));
        }
        return response;
    }

    @Transactional
    public GetAnnouncementForAdminResponse updateAnnouncementForAdmin(Long announcementId, UpdateAnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_ANNOUNCEMENT));
        announcement.update(request.title().trim(), request.context().trim(), request.expiredAt());
        return toAdminAnnouncementResponse(announcement, clock.instant());
    }

    @Transactional
    public void deleteAnnouncementForAdmin(Long announcementId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_ANNOUNCEMENT));
        announcementRepository.delete(announcement);
    }

    // ===== Admin: system info =====

    @Transactional(readOnly = true)
    public GetSystemInfoForAdminResponse getSystemInfoForAdmin() {
        return toSystemInfoResponse(loadSystemInfo());
    }

    @Transactional
    public GetSystemInfoForAdminResponse updateSystemInfoForAdmin(UpdateSystemInfoRequest request) {
        SystemInfo systemInfo = loadSystemInfo();
        // Stored trimmed because getPersonalNotice compares against the trimmed client version
        systemInfo.update(request.version().trim(), request.isSystemCheck());
        return toSystemInfoResponse(systemInfo);
    }

    // ===== Admin: personal notice =====

    /**
     * Looks up a non-deleted user by exact email first, then by exact nickname.
     */
    @Transactional(readOnly = true)
    public GetNoticeRecipientResponse findNoticeRecipientForAdmin(String keyword) {
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.isEmpty()) {
            throw new CustomException("Keyword is required", ErrorCode.VALIDATION_ERROR);
        }
        UserEntity user = userRepository.findByEmail(trimmed)
                .or(() -> userRepository.findByNickname(trimmed))
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));
        return toRecipientResponse(user);
    }

    @Transactional
    public GetNoticeRecipientResponse sendPersonalNoticeForAdmin(SendPersonalNoticeRequest request) {
        UserEntity user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));
        noticeRepository.save(Notice.builder()
                .user(user)
                .context(request.context().trim())
                .build());
        return toRecipientResponse(user);
    }

    // ===== helpers =====

    private SystemInfo loadSystemInfo() {
        return systemInfoRepository.getSystemInfo()
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private GetAnnouncementForAdminResponse toAdminAnnouncementResponse(Announcement announcement, Instant now) {
        Instant createdAt = announcement.getCreatedAt();
        Instant expiredAt = announcement.getExpiredAt();
        return GetAnnouncementForAdminResponse.builder()
                .id(announcement.getId())
                .langCode(announcement.getLangCode().getName())
                .title(announcement.getTitle())
                .context(announcement.getContext())
                .createdAt(createdAt == null ? null : createdAt.toString())
                .expiredAt(expiredAt == null ? null : expiredAt.toString())
                .isActive(expiredAt != null && expiredAt.isAfter(now))
                .build();
    }

    private GetSystemInfoForAdminResponse toSystemInfoResponse(SystemInfo systemInfo) {
        return GetSystemInfoForAdminResponse.builder()
                .version(systemInfo.getVersion())
                .isSystemCheck(systemInfo.isSystemCheck())
                .build();
    }

    private GetNoticeRecipientResponse toRecipientResponse(UserEntity user) {
        return GetNoticeRecipientResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

}
