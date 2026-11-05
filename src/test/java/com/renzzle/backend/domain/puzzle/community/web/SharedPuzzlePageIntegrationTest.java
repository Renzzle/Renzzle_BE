package com.renzzle.backend.domain.puzzle.community.web;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.appinfo.api.request.UpsertAppInfoRequest;
import com.renzzle.backend.domain.appinfo.dao.AppInfoRepository;
import com.renzzle.backend.domain.appinfo.service.AppInfoService;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.service.CommunityShareService;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.support.TestCommunityPuzzleBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
class SharedPuzzlePageIntegrationTest {

    private static final String BOARD = "h8i8h9i9h10i10";
    private static final String ANSWER = "h11h12h7";
    private static final List<String> STORE_TAGS = List.of("ios_store_url", "android_store_url");
    private static final List<String> PAGE_LANGS = List.of("ko", "en", "ja", "zh-Hans", "zh-Hant", "ru", "et");

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityPuzzleRepository communityPuzzleRepository;
    @Autowired private CommunityShareService communityShareService;
    @Autowired private AppInfoService appInfoService;
    @Autowired private AppInfoRepository appInfoRepository;

    private UserEntity user;
    private Long puzzleId;
    private String token;

    @BeforeEach
    void setUp() {
        user = userRepository.save(
                TestUserFactory.createTestUser("page-" + UUID.randomUUID().toString().substring(0, 8), 1000));
        puzzleId = TestCommunityPuzzleBuilder.builder(user)
                .withBoardStatus(BOARD)
                .withAnswer(ANSWER)
                .withView(3)
                .withSolvedCount(5)
                .withLikeCount(7)
                .save(communityPuzzleRepository)
                .getId();
        String sharePath = communityShareService.shareCommunityPuzzle(puzzleId).sharePath();
        token = sharePath.substring(sharePath.lastIndexOf('/') + 1);
    }

    @AfterEach
    void tearDown() {
        STORE_TAGS.forEach(tag -> appInfoRepository.findByTag(tag).ifPresent(appInfoRepository::delete));
        userRepository.delete(user);
    }

    @Test
    void page_WithoutLogin_ThenRendersTheBoardAndPreviewTagsInKorean() throws Exception {
        String html = page(token, "ko-KR,ko;q=0.9", 200);

        assertThat(html)
                .contains("<html lang=\"ko\"")
                .contains("property=\"og:title\" content=\"" + user.getNickname() + "님의 오목 퍼즐\"")
                .contains("data-token=\"" + token + "\"")
                .contains("data-board=\"" + BOARD + "\"")
                .contains("흑으로 오목을 만드세요")
                .contains("풀이 5 · 조회 3")
                .contains("aria-label=\"좋아요 7\"")
                .contains("id=\"result\"")
                .contains("세로 화면으로 돌려 주세요")
                .contains("property=\"og:url\" content=\"https://renzzle.com/puzzle/" + token + "\"")
                .contains("property=\"og:image\" content=\"https://renzzle.com/assets/share/og-image-ko.png?v=1\"")
                .doesNotContain(ANSWER)
                .doesNotContain("apple-itunes-app");
        assertThat(communityPuzzleRepository.findById(puzzleId).orElseThrow().getView()).isEqualTo(3);
    }

    @Test
    void page_WhenServedFromAnotherAddress_ThenPreviewLinksFollowIt() throws Exception {
        String html = page("https://test.renzzle.com:443", token, null, 200);

        assertThat(html)
                .contains("property=\"og:url\" content=\"https://test.renzzle.com/puzzle/" + token + "\"")
                .contains("property=\"og:image\" content=\"https://test.renzzle.com/assets/share/og-image-en.png?v=1\"");
    }

    @Test
    void page_WhenBrowserPrefersEnglish_ThenRendersInEnglish() throws Exception {
        String html = page(token, "en-US,en;q=0.9,ko;q=0.8", 200);

        assertThat(html)
                .contains("<html lang=\"en\"")
                .contains("content=\"" + user.getNickname() + "&#39;s Renju puzzle\"")
                .contains("Make five with black")
                .contains("solved 5 · views 3")
                .contains("property=\"og:image\" content=\"https://renzzle.com/assets/share/og-image-en.png?v=1\"");
    }

    // Link preview crawlers often send no Accept-Language
    @Test
    void page_WithoutLanguageHeader_ThenRendersInEnglish() throws Exception {
        String html = page(token, null, 200);

        assertThat(html)
                .contains("<html lang=\"en\"")
                .contains("content=\"" + user.getNickname() + "&#39;s Renju puzzle\"")
                .contains("property=\"og:image\" content=\"https://renzzle.com/assets/share/og-image-en.png?v=1\"");
    }

    @Test
    void page_WhenTheBrowserAndTheLinkNameLanguages_ThenRendersInTheBrowsersAndKeepsTheLinksInOgUrl() throws Exception {
        String html = page(token + "?lang=ja", "ko-KR,ko;q=0.9", 200);

        assertThat(html)
                .contains("<html lang=\"ko\"")
                .contains("property=\"og:url\" content=\"https://renzzle.com/puzzle/" + token + "?lang=ja\"")
                .contains("property=\"og:image\" content=\"https://renzzle.com/assets/share/og-image-ko.png?v=1\"");
    }

    // Link preview crawlers often send no Accept-Language
    @Test
    void page_WhenTheLinkNamesALanguageAndTheBrowserNone_ThenRendersInIt() throws Exception {
        String html = page(token + "?lang=ja", null, 200);

        assertThat(html)
                .contains("<html lang=\"ja\"")
                .contains("property=\"og:url\" content=\"https://renzzle.com/puzzle/" + token + "?lang=ja\"")
                .contains("property=\"og:image\" content=\"https://renzzle.com/assets/share/og-image-ja.png?v=1\"");
    }

    @Test
    void page_WhenTheLinkNamesALanguageThePageLacks_ThenFollowsTheBrowser() throws Exception {
        String html = page(token + "?lang=fr", "ko-KR,ko;q=0.9", 200);

        assertThat(html)
                .contains("<html lang=\"ko\"")
                .contains("property=\"og:url\" content=\"https://renzzle.com/puzzle/" + token + "\"");
    }

    @Test
    void page_WhenBrowserPrefersTraditionalChinese_ThenRendersInIt() throws Exception {
        String html = page(token, "zh-TW,zh;q=0.9,en;q=0.8", 200);

        assertThat(html)
                .contains("<html lang=\"zh-Hant\"")
                .contains("執黑連成五子")
                .contains(user.getNickname() + "</b> 出題")
                .contains("property=\"og:image\" content=\"https://renzzle.com/assets/share/og-image-zh-Hant.png?v=1\"");
    }

    @Test
    void page_WhenTokenUnknown_ThenNotFoundWithoutTheEngine() throws Exception {
        String html = page(UUID.randomUUID().toString(), null, 404);

        assertThat(html)
                .contains("Puzzle not found")
                .doesNotContain("/assets/engine/engine.js")
                .contains("property=\"og:image\" content=\"https://renzzle.com/assets/share/og-image-en.png?v=1\"");
    }

    @Test
    void previewImages_WithoutLogin_ThenAreServedAsWidePngsInEveryPageLanguage() throws Exception {
        for (String lang : PAGE_LANGS) {
            MvcResult result = mockMvc.perform(get("/assets/share/og-image-" + lang + ".png"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG))
                    .andReturn();

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
            assertThat(image.getWidth()).as(lang).isEqualTo(1200);
            assertThat(image.getHeight()).as(lang).isEqualTo(600);
        }
    }

    @Test
    void page_WhenStoreLinksAreSet_ThenLinksAppleDevicesAndMacsToTheAppStoreAndTheRestToGooglePlay() throws Exception {
        appInfoService.upsertAppInfo(new UpsertAppInfoRequest("ios_store_url", "https://apps.apple.com/app/renzzle/id1234567890"));
        appInfoService.upsertAppInfo(new UpsertAppInfoRequest("android_store_url", "https://play.google.com/store/apps/details?id=com.renzzle_fe"));

        assertThat(pageOn("Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15"))
                .contains("href=\"https://apps.apple.com/app/renzzle/id1234567890\"")
                .doesNotContain("play.google.com");
        assertThat(pageOn("Mozilla/5.0 (Linux; Android 15; SM-S938N) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36"))
                .contains("href=\"https://play.google.com/store/apps/details?id=com.renzzle_fe\"")
                .doesNotContain("apps.apple.com");
        // A Mac, or an iPad, whose Safari asks for the desktop site by default
        assertThat(pageOn("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/18.0 Safari/605.1.15"))
                .contains("href=\"https://apps.apple.com/app/renzzle/id1234567890\"")
                .doesNotContain("play.google.com");
        assertThat(pageOn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36"))
                .contains("href=\"https://play.google.com/store/apps/details?id=com.renzzle_fe\"")
                .doesNotContain("apps.apple.com");
    }

    private String pageOn(String userAgent) throws Exception {
        return mockMvc.perform(get("/puzzle/" + token).header(HttpHeaders.USER_AGENT, userAgent))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // Behind Cloudflare and Caddy, Tomcat turns X-Forwarded-Proto into https on port 443 (server.forward-headers-strategy)
    private String page(String shareToken, String acceptLanguage, int expectedStatus) throws Exception {
        return page("https://renzzle.com:443", shareToken, acceptLanguage, expectedStatus);
    }

    private String page(String origin, String shareToken, String acceptLanguage, int expectedStatus) throws Exception {
        var request = get(URI.create(origin + "/puzzle/" + shareToken)).accept(MediaType.TEXT_HTML);
        if (acceptLanguage != null) {
            request.header(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage);
        }
        return mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }
}
