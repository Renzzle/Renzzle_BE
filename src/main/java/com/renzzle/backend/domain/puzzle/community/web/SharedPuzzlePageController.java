package com.renzzle.backend.domain.puzzle.community.web;

import com.renzzle.backend.domain.appinfo.service.AppInfoService;
import com.renzzle.backend.domain.puzzle.community.api.response.GetSharedPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.service.CommunityShareService;
import com.renzzle.backend.global.common.domain.LangCode.LangCodeName;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class SharedPuzzlePageController {

    private static final String IOS_STORE_URL_TAG = "ios_store_url";
    private static final String ANDROID_STORE_URL_TAG = "android_store_url";
    // Bump when the og-image files change, since KakaoTalk caches previews by URL
    private static final int PREVIEW_VERSION = 1;

    private final CommunityShareService communityShareService;
    private final AppInfoService appInfoService;

    @Value("${community.share.preview-image-path}")
    private String previewImagePath;

    @GetMapping("/puzzle/{token}")
    public String sharedPuzzlePage(
            @PathVariable String token,
            @RequestParam(required = false) String lang,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            Model model,
            HttpServletResponse response
    ) {
        Optional<GetSharedPuzzleResponse> puzzle = communityShareService.findSharedPuzzle(token);
        if (puzzle.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

        // ?lang= set by the app comes first, as link previews often send no Accept-Language
        LangCodeName linkLanguage = SharedPuzzlePageText.languageOfParam(lang);
        LangCodeName language = linkLanguage != null ? linkLanguage : SharedPuzzlePageText.languageOf(acceptLanguage);
        SharedPuzzlePageText text = SharedPuzzlePageText.of(language, puzzle.orElse(null));
        model.addAttribute("text", text);
        model.addAttribute("token", token);
        model.addAttribute("puzzle", puzzle.orElse(null));
        // Absolute, as link previews need; the address the visitor or crawler used, already https behind the proxies.
        // Keeps the language, as some crawlers fetch og:url again
        UriComponentsBuilder pageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(communityShareService.sharePathOf(token));
        if (linkLanguage != null) {
            pageUrl.queryParam("lang", text.lang());
        }
        model.addAttribute("pageUrl", pageUrl.toUriString());
        model.addAttribute("previewImageUrl", ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(previewImagePath.formatted(text.lang()))
                .queryParam("v", PREVIEW_VERSION)
                .toUriString());
        model.addAttribute("storeUrl", storeUrlFor(userAgent));
        return "share/puzzle";
    }

    // The App Store on Apple devices and Macs (iPad Safari asks as a Mac by default), Google Play everywhere else
    private String storeUrlFor(String userAgent) {
        boolean apple = userAgent != null && (userAgent.contains("iPhone") || userAgent.contains("iPad")
                || userAgent.contains("iPod") || userAgent.contains("Macintosh"));
        return findStoreUrl(apple ? IOS_STORE_URL_TAG : ANDROID_STORE_URL_TAG);
    }

    private String findStoreUrl(String tag) {
        return appInfoService.findValue(tag)
                .filter(url -> url.startsWith("https://"))
                .orElse(null);
    }
}
