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
import com.renzzle.backend.domain.notice.util.NoticeTextBuilderUtil;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.Title;

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
        if (userRepository.isLastAccessBeforeToday(user.getId())) {
            int price = 200;
            userRepository.addUserCurrency(user.getId(), price);
            contexts.add(NoticeContext.builder()
                    .context(NoticeTextBuilderUtil.buildAttendanceMessage(LangCode.getLangCode(request.langCode()), price))
                    .build()
            );
        }
        userRepository.updateLastAccessedAt(user.getId(), clock.instant());

        // 3. Title notification
        Title userTitle = userRepository.getUserTitle(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));
        Title none = Title.getTitle(Title.TitleType.NONE);
        Title masterTitle = Title.getTitle(Title.TitleType.MASTER);
        Title grandmasterTitle = Title.getTitle(Title.TitleType.GRANDMASTER);

        if (userTitle.equals(none)) { // check master title available
            boolean isMasterValid = userRepository.isUserQualified(user.getId(), 100, 20, 0.0, 200);
            if (isMasterValid) {
                userRepository.updateUserTitle(user.getId(), masterTitle);
                contexts.add(NoticeContext.builder()
                        .context(NoticeTextBuilderUtil.buildGetTitleMessage(LangCode.getLangCode(request.langCode()), masterTitle))
                        .build()
                );
            }
        } else if (userTitle.equals(masterTitle)) { // check grandmaster title available
            boolean isGrandMasterValid = userRepository.isUserQualified(user.getId(), 1000, 200, 0.0, 2000);
            if (isGrandMasterValid) {
                userRepository.updateUserTitle(user.getId(), grandmasterTitle);
                contexts.add(NoticeContext.builder()
                        .context(NoticeTextBuilderUtil.buildGetTitleMessage(LangCode.getLangCode(request.langCode()), grandmasterTitle))
                        .build()
                );
            }
        }

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
