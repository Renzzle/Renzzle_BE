package com.renzzle.backend.domain.notice.service;

import com.renzzle.backend.domain.notice.api.request.GetPersonalNoticeRequest;
import com.renzzle.backend.domain.notice.api.request.GetPublicNoticeRequest;
import com.renzzle.backend.domain.notice.api.response.GetPersonalNoticeResponse;
import com.renzzle.backend.domain.notice.api.response.GetPublicNoticeResponse;
import com.renzzle.backend.domain.notice.api.response.NoticeContext;
import com.renzzle.backend.domain.notice.dao.AnnouncementRepository;
import com.renzzle.backend.domain.notice.dao.NoticeRepository;
import com.renzzle.backend.domain.notice.dao.SystemInfoRepository;
import com.renzzle.backend.domain.notice.domain.Announcement;
import com.renzzle.backend.domain.notice.domain.Notice;
import com.renzzle.backend.domain.notice.domain.SystemInfo;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final Clock clock;
    private final NoticeRepository noticeRepository;
    private final AnnouncementRepository announcementRepository;
    private final SystemInfoRepository systemInfoRepository;

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

        // 3. Badge price

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

}
