package com.renzzle.backend.domain.notice.service;

import com.renzzle.backend.domain.notice.api.request.AnnouncementContentRequest;
import com.renzzle.backend.domain.notice.api.request.CreateAnnouncementRequest;
import com.renzzle.backend.domain.notice.api.request.GetPersonalNoticeRequest;
import com.renzzle.backend.domain.notice.api.request.SendPersonalNoticeRequest;
import com.renzzle.backend.domain.notice.api.request.UpdateAnnouncementRequest;
import com.renzzle.backend.domain.notice.api.request.UpdateSystemInfoRequest;
import com.renzzle.backend.domain.notice.api.response.GetAnnouncementForAdminResponse;
import com.renzzle.backend.domain.notice.api.response.GetNoticeRecipientResponse;
import com.renzzle.backend.domain.notice.api.response.GetPersonalNoticeResponse;
import com.renzzle.backend.domain.notice.api.response.GetSystemInfoForAdminResponse;
import com.renzzle.backend.domain.notice.api.response.NoticeContext;
import com.renzzle.backend.domain.notice.dao.AnnouncementRepository;
import com.renzzle.backend.domain.notice.dao.NoticeRepository;
import com.renzzle.backend.domain.notice.dao.SystemInfoRepository;
import com.renzzle.backend.domain.notice.domain.Announcement;
import com.renzzle.backend.domain.notice.domain.Notice;
import com.renzzle.backend.domain.notice.domain.SystemInfo;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static com.renzzle.backend.support.TestTime.FIXED_INSTANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private Clock clock;
    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private SystemInfoRepository systemInfoRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NoticeService noticeService;

    private UserEntity user;

    @BeforeEach
    void setup() {
        lenient().when(clock.instant()).thenReturn(FIXED_INSTANT);
        user = TestUserEntityBuilder.builder()
                .withId(7L)
                .withEmail("player@test.com")
                .withNickname("player")
                .withStatus(Status.getDefaultStatus())
                .build();
    }

    private static SystemInfo systemInfo(String androidVersion, String iosVersion, boolean isSystemCheck) {
        return SystemInfo.builder()
                .androidVersion(androidVersion)
                .iosVersion(iosVersion)
                .isSystemCheck(isSystemCheck)
                .build();
    }

    private static Announcement announcement(Long id, String lang, Instant expiredAt) {
        return Announcement.builder()
                .id(id)
                .langCode(LangCode.getLangCode(lang))
                .title("Title " + id)
                .context("Context " + id)
                .createdAt(FIXED_INSTANT.minus(1, ChronoUnit.DAYS))
                .expiredAt(expiredAt)
                .build();
    }

    // ===== announcements: list =====

    @Test
    void getAnnouncementsForAdmin_WhenNoLangFilter_ThenReturnsAllWithActiveFlag() {
        // Given
        Announcement active = announcement(1L, "EN", FIXED_INSTANT.plus(1, ChronoUnit.DAYS));
        Announcement expired = announcement(2L, "KO", FIXED_INSTANT.minus(1, ChronoUnit.HOURS));
        when(announcementRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(active, expired));

        // When
        List<GetAnnouncementForAdminResponse> response = noticeService.getAnnouncementsForAdmin(null);

        // Then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).id()).isEqualTo(1L);
        assertThat(response.get(0).langCode()).isEqualTo("EN");
        assertThat(response.get(0).isActive()).isTrue();
        assertThat(response.get(0).createdAt()).isEqualTo(active.getCreatedAt().toString());
        assertThat(response.get(0).expiredAt()).isEqualTo(active.getExpiredAt().toString());
        assertThat(response.get(1).id()).isEqualTo(2L);
        assertThat(response.get(1).langCode()).isEqualTo("KO");
        assertThat(response.get(1).isActive()).isFalse();
        verify(announcementRepository, never()).findAllByLangCodeOrderByCreatedAtDescIdDesc(any());
    }

    @Test
    void getAnnouncementsForAdmin_WhenLangFilterGiven_ThenQueriesByLang() {
        // Given
        Announcement ko = announcement(3L, "KO", FIXED_INSTANT.plus(1, ChronoUnit.DAYS));
        when(announcementRepository.findAllByLangCodeOrderByCreatedAtDescIdDesc(any())).thenReturn(List.of(ko));

        // When
        List<GetAnnouncementForAdminResponse> response = noticeService.getAnnouncementsForAdmin("ko");

        // Then
        ArgumentCaptor<LangCode> captor = ArgumentCaptor.forClass(LangCode.class);
        verify(announcementRepository).findAllByLangCodeOrderByCreatedAtDescIdDesc(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("KO");
        verify(announcementRepository, never()).findAllByOrderByCreatedAtDescIdDesc();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).langCode()).isEqualTo("KO");
    }

    // ===== announcements: create =====

    @Test
    void createAnnouncementsForAdmin_WhenTwoLanguages_ThenSavesOneRowPerLanguage() {
        // Given
        Instant expiredAt = FIXED_INSTANT.plus(7, ChronoUnit.DAYS);
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(expiredAt, List.of(
                new AnnouncementContentRequest("EN", "  Maintenance  ", "  Servers go down at 3am.  "),
                new AnnouncementContentRequest("KO", "점검 안내", "새벽 3시에 점검합니다.")
        ));
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> {
            Announcement arg = invocation.getArgument(0);
            long id = "EN".equals(arg.getLangCode().getName()) ? 10L : 11L;
            return arg.toBuilder().id(id).createdAt(FIXED_INSTANT).build();
        });

        // When
        List<GetAnnouncementForAdminResponse> response = noticeService.createAnnouncementsForAdmin(request);

        // Then
        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository, times(2)).save(captor.capture());
        List<Announcement> saved = captor.getAllValues();
        assertThat(saved).extracting(a -> a.getLangCode().getName()).containsExactly("EN", "KO");
        assertThat(saved.get(0).getTitle()).isEqualTo("Maintenance");
        assertThat(saved.get(0).getContext()).isEqualTo("Servers go down at 3am.");
        assertThat(saved).allSatisfy(a -> assertThat(a.getExpiredAt()).isEqualTo(expiredAt));

        assertThat(response).hasSize(2);
        assertThat(response).extracting(GetAnnouncementForAdminResponse::id).containsExactly(10L, 11L);
        assertThat(response).allSatisfy(r -> assertThat(r.isActive()).isTrue());
    }

    @Test
    void createAnnouncementsForAdmin_WhenDuplicateLanguage_ThenThrowsValidationError() {
        // Given
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(FIXED_INSTANT.plus(1, ChronoUnit.DAYS), List.of(
                new AnnouncementContentRequest("EN", "A", "a"),
                new AnnouncementContentRequest("en", "B", "b")
        ));

        // When
        CustomException exception = assertThrows(CustomException.class,
                () -> noticeService.createAnnouncementsForAdmin(request));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(announcementRepository, never()).save(any());
    }

    // ===== announcements: update / delete =====

    @Test
    void updateAnnouncementForAdmin_WhenExists_ThenUpdatesFieldsAndRecomputesActive() {
        // Given
        Announcement existing = announcement(5L, "EN", FIXED_INSTANT.plus(1, ChronoUnit.DAYS));
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(existing));
        Instant pastExpiry = FIXED_INSTANT.minus(1, ChronoUnit.MINUTES);
        UpdateAnnouncementRequest request = new UpdateAnnouncementRequest(" New title ", " New body ", pastExpiry);

        // When
        GetAnnouncementForAdminResponse response = noticeService.updateAnnouncementForAdmin(5L, request);

        // Then
        assertThat(existing.getTitle()).isEqualTo("New title");
        assertThat(existing.getContext()).isEqualTo("New body");
        assertThat(existing.getExpiredAt()).isEqualTo(pastExpiry);
        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.isActive()).isFalse();
    }

    @Test
    void updateAnnouncementForAdmin_WhenNotFound_ThenThrows() {
        // Given
        when(announcementRepository.findById(99L)).thenReturn(Optional.empty());
        UpdateAnnouncementRequest request = new UpdateAnnouncementRequest("t", "c", FIXED_INSTANT);

        // When
        CustomException exception = assertThrows(CustomException.class,
                () -> noticeService.updateAnnouncementForAdmin(99L, request));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_FIND_ANNOUNCEMENT);
    }

    @Test
    void deleteAnnouncementForAdmin_WhenExists_ThenDeletes() {
        // Given
        Announcement existing = announcement(6L, "EN", FIXED_INSTANT.plus(1, ChronoUnit.DAYS));
        when(announcementRepository.findById(6L)).thenReturn(Optional.of(existing));

        // When
        noticeService.deleteAnnouncementForAdmin(6L);

        // Then
        verify(announcementRepository).delete(existing);
    }

    @Test
    void deleteAnnouncementForAdmin_WhenNotFound_ThenThrows() {
        // Given
        when(announcementRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        CustomException exception = assertThrows(CustomException.class,
                () -> noticeService.deleteAnnouncementForAdmin(99L));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_FIND_ANNOUNCEMENT);
        verify(announcementRepository, never()).delete(any());
    }

    // ===== system info =====

    @Test
    void getSystemInfoForAdmin_WhenExists_ThenReturnsBothPlatformVersions() {
        // Given
        when(systemInfoRepository.getSystemInfo()).thenReturn(Optional.of(systemInfo("1.0.0", "1.0.3", false)));

        // When
        GetSystemInfoForAdminResponse response = noticeService.getSystemInfoForAdmin();

        // Then
        assertThat(response.androidVersion()).isEqualTo("1.0.0");
        assertThat(response.iosVersion()).isEqualTo("1.0.3");
        assertThat(response.isSystemCheck()).isFalse();
    }

    @Test
    void updateSystemInfoForAdmin_WhenCalled_ThenUpdatesTrimmedVersionsAndFlag() {
        // Given
        SystemInfo stored = systemInfo("1.0.0", "1.0.0", false);
        when(systemInfoRepository.getSystemInfo()).thenReturn(Optional.of(stored));

        // When
        GetSystemInfoForAdminResponse response = noticeService.updateSystemInfoForAdmin(
                new UpdateSystemInfoRequest("  1.2.0  ", "  1.2.1  ", true));

        // Then
        assertThat(stored.getAndroidVersion()).isEqualTo("1.2.0");
        assertThat(stored.getIosVersion()).isEqualTo("1.2.1");
        assertThat(stored.isSystemCheck()).isTrue();
        assertThat(response.androidVersion()).isEqualTo("1.2.0");
        assertThat(response.iosVersion()).isEqualTo("1.2.1");
        assertThat(response.isSystemCheck()).isTrue();
    }

    @Test
    void updateSystemInfoForAdmin_WhenSystemInfoMissing_ThenThrowsInternalError() {
        // Given
        when(systemInfoRepository.getSystemInfo()).thenReturn(Optional.empty());

        // When
        CustomException exception = assertThrows(CustomException.class,
                () -> noticeService.updateSystemInfoForAdmin(new UpdateSystemInfoRequest("1.0.0", "1.0.0", false)));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    // ===== personal notice: per-platform version gate =====

    @Test
    void getPersonalNotice_WhenAndroidVersionStale_ThenAsksToUpdateWithAndroidVersion() {
        // Given: iOS differs, so reading the wrong platform could not yield 1.0.5
        when(systemInfoRepository.getSystemInfo()).thenReturn(Optional.of(systemInfo("1.0.5", "1.0.9", false)));

        // When
        GetPersonalNoticeResponse response = noticeService.getPersonalNotice(
                new GetPersonalNoticeRequest("EN", "ANDROID", "1.0.4"), user);

        // Then
        assertThat(response.description()).isEqualTo("update");
        assertThat(response.version()).isEqualTo("1.0.5");
        verify(noticeRepository, never()).findAllByUser(any());
    }

    @Test
    void getPersonalNotice_WhenIosClientOnAndroidVersion_ThenStillAsksToUpdate() {
        // Given
        when(systemInfoRepository.getSystemInfo()).thenReturn(Optional.of(systemInfo("1.0.5", "1.0.9", false)));

        // When: client sends the Android-required version but identifies as iOS
        GetPersonalNoticeResponse response = noticeService.getPersonalNotice(
                new GetPersonalNoticeRequest("EN", "ios", "1.0.5"), user);

        // Then
        assertThat(response.description()).isEqualTo("update");
        assertThat(response.version()).isEqualTo("1.0.9");
    }

    @Test
    void getPersonalNotice_WhenPlatformVersionMatches_ThenReturnsContexts() {
        // Given
        when(systemInfoRepository.getSystemInfo()).thenReturn(Optional.of(systemInfo("1.0.5", "1.0.9", false)));
        when(noticeRepository.findAllByUser(user)).thenReturn(List.of(
                Notice.builder().user(user).context("hello").build()));
        when(userRepository.isLastAccessBeforeToday(user.getId())).thenReturn(false);

        // When
        GetPersonalNoticeResponse response = noticeService.getPersonalNotice(
                new GetPersonalNoticeRequest("EN", "IOS", " 1.0.9 "), user);

        // Then
        assertThat(response.description()).isEqualTo("context");
        assertThat(response.notice()).extracting(NoticeContext::context).containsExactly("hello");
        verify(noticeRepository).deleteAllByUser(user);
    }

    @Test
    void getPersonalNotice_WhenSystemCheckOn_ThenShortCircuitsBeforeVersionCheck() {
        // Given
        when(systemInfoRepository.getSystemInfo()).thenReturn(Optional.of(systemInfo("1.0.5", "1.0.9", true)));

        // When
        GetPersonalNoticeResponse response = noticeService.getPersonalNotice(
                new GetPersonalNoticeRequest("EN", "ANDROID", "0.0.1"), user);

        // Then
        assertThat(response.description()).isEqualTo("system-check");
        assertThat(response.version()).isNull();
    }

    @Test
    void getPersonalNotice_WhenPlatformUnknown_ThenThrows() {
        // Given
        when(systemInfoRepository.getSystemInfo()).thenReturn(Optional.of(systemInfo("1.0.5", "1.0.9", false)));

        // When / Then: bean validation rejects this in the web layer first; the service
        // must not quietly fall back to a default platform either.
        assertThrows(IllegalArgumentException.class, () -> noticeService.getPersonalNotice(
                new GetPersonalNoticeRequest("EN", "WINDOWS", "1.0.5"), user));
    }

    // ===== personal notice: recipient lookup =====

    @Test
    void findNoticeRecipientForAdmin_WhenEmailMatches_ThenReturnsUserWithoutNicknameLookup() {
        // Given
        when(userRepository.findByEmail("player@test.com")).thenReturn(Optional.of(user));

        // When
        GetNoticeRecipientResponse response = noticeService.findNoticeRecipientForAdmin("  player@test.com ");

        // Then
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.email()).isEqualTo("player@test.com");
        assertThat(response.nickname()).isEqualTo("player");
        verify(userRepository, never()).findByNickname(anyString());
    }

    @Test
    void findNoticeRecipientForAdmin_WhenOnlyNicknameMatches_ThenReturnsUser() {
        // Given
        when(userRepository.findByEmail("player")).thenReturn(Optional.empty());
        when(userRepository.findByNickname("player")).thenReturn(Optional.of(user));

        // When
        GetNoticeRecipientResponse response = noticeService.findNoticeRecipientForAdmin("player");

        // Then
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.nickname()).isEqualTo("player");
    }

    @Test
    void findNoticeRecipientForAdmin_WhenNoMatch_ThenThrowsCannotFindUser() {
        // Given
        when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByNickname("ghost")).thenReturn(Optional.empty());

        // When
        CustomException exception = assertThrows(CustomException.class,
                () -> noticeService.findNoticeRecipientForAdmin("ghost"));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_FIND_USER);
    }

    @Test
    void findNoticeRecipientForAdmin_WhenBlankKeyword_ThenThrowsValidationErrorWithoutLookup() {
        // When
        CustomException exception = assertThrows(CustomException.class,
                () -> noticeService.findNoticeRecipientForAdmin("   "));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).findByNickname(anyString());
    }

    // ===== personal notice: send =====

    @Test
    void sendPersonalNoticeForAdmin_WhenUserExists_ThenSavesTrimmedNoticeForUser() {
        // Given
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        SendPersonalNoticeRequest request = new SendPersonalNoticeRequest(7L, "  You won a prize!  ");

        // When
        GetNoticeRecipientResponse response = noticeService.sendPersonalNoticeForAdmin(request);

        // Then
        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getContext()).isEqualTo("You won a prize!");
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.nickname()).isEqualTo("player");
    }

    @Test
    void sendPersonalNoticeForAdmin_WhenUserMissing_ThenThrowsAndSavesNothing() {
        // Given
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        // When
        CustomException exception = assertThrows(CustomException.class,
                () -> noticeService.sendPersonalNoticeForAdmin(new SendPersonalNoticeRequest(404L, "hello")));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_FIND_USER);
        verify(noticeRepository, never()).save(any());
    }

}
