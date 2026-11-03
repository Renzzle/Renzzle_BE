package com.renzzle.backend.domain.puzzle.community.web;

import com.renzzle.backend.domain.appinfo.service.AppInfoService;
import com.renzzle.backend.domain.puzzle.community.api.response.GetSharedPuzzleResponse;
import com.renzzle.backend.domain.puzzle.community.service.CommunityShareService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class SharedPuzzlePageController {

    private static final String IOS_STORE_URL_TAG = "ios_store_url";
    private static final String ANDROID_STORE_URL_TAG = "android_store_url";
    // Bump when the preview image changes, since KakaoTalk caches previews by URL
    private static final int PREVIEW_VERSION = 2;

    private final CommunityShareService communityShareService;
    private final AppInfoService appInfoService;

    @GetMapping("/puzzle/{token}")
    public String sharedPuzzlePage(
            @PathVariable String token,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            Model model,
            HttpServletResponse response
    ) {
        Optional<GetSharedPuzzleResponse> puzzle = communityShareService.findSharedPuzzle(token);
        if (puzzle.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

        model.addAttribute("text", SharedPuzzlePageText.of(SharedPuzzlePageText.languageOf(acceptLanguage), puzzle.orElse(null)));
        model.addAttribute("token", token);
        model.addAttribute("puzzle", puzzle.orElse(null));
        // Absolute, as link previews need; the address the visitor or crawler used, already https behind the proxies
        String pageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(communityShareService.sharePathOf(token))
                .toUriString();
        model.addAttribute("pageUrl", pageUrl);
        model.addAttribute("previewImageUrl", puzzle.isPresent()
                ? pageUrl + "/preview.png?v=" + PREVIEW_VERSION
                : null);
        model.addAttribute("storeUrl", storeUrlFor(userAgent));
        return "share/puzzle";
    }

    // The store of the visitor's phone; none on a computer, where the app can't be installed
    private String storeUrlFor(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        if (userAgent.contains("iPhone") || userAgent.contains("iPad") || userAgent.contains("iPod")) {
            return findStoreUrl(IOS_STORE_URL_TAG);
        }
        return userAgent.contains("Android") ? findStoreUrl(ANDROID_STORE_URL_TAG) : null;
    }

    private String findStoreUrl(String tag) {
        return appInfoService.findValue(tag)
                .filter(url -> url.startsWith("https://"))
                .orElse(null);
    }
}
