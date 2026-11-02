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

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
public class SharedPuzzlePageController {

    private static final String IOS_STORE_URL_TAG = "ios_store_url";
    private static final String ANDROID_STORE_URL_TAG = "android_store_url";
    private static final Pattern APP_STORE_ID = Pattern.compile("/id(\\d+)");
    // Bump when the preview image changes, since KakaoTalk caches previews by URL
    private static final int PREVIEW_VERSION = 1;

    private final CommunityShareService communityShareService;
    private final AppInfoService appInfoService;

    @GetMapping("/puzzle/{token}")
    public String sharedPuzzlePage(
            @PathVariable String token,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            Model model,
            HttpServletResponse response
    ) {
        boolean korean = prefersKorean(acceptLanguage);
        Optional<GetSharedPuzzleResponse> puzzle = communityShareService.findSharedPuzzle(token);
        if (puzzle.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

        String iosStoreUrl = findStoreUrl(IOS_STORE_URL_TAG);
        model.addAttribute("korean", korean);
        model.addAttribute("token", token);
        model.addAttribute("puzzle", puzzle.orElse(null));
        model.addAttribute("pageTitle", puzzle.map(found -> titleOf(found, korean))
                .orElse(korean ? "퍼즐을 찾을 수 없어요" : "Puzzle not found"));
        model.addAttribute("pageDescription", puzzle.map(found -> descriptionOf(found, korean))
                .orElse(korean ? "삭제되었거나 잘못된 링크예요." : "It was removed, or the link is wrong."));
        model.addAttribute("pageUrl", communityShareService.shareUrlOf(token));
        model.addAttribute("previewImageUrl", puzzle.isPresent()
                ? communityShareService.shareUrlOf(token) + "/preview.png?v=" + PREVIEW_VERSION
                : null);
        model.addAttribute("iosStoreUrl", iosStoreUrl);
        model.addAttribute("androidStoreUrl", findStoreUrl(ANDROID_STORE_URL_TAG));
        model.addAttribute("appStoreId", appStoreIdOf(iosStoreUrl));
        return "share/puzzle";
    }

    // Korean unless the browser asks for another language first, since link previews often send no header
    private boolean prefersKorean(String acceptLanguage) {
        return acceptLanguage == null || acceptLanguage.isBlank()
                || acceptLanguage.trim().toLowerCase(Locale.ROOT).startsWith("ko");
    }

    private String titleOf(GetSharedPuzzleResponse puzzle, boolean korean) {
        return korean ? puzzle.authorName() + "님의 오목 퍼즐" : puzzle.authorName() + "'s Gomoku puzzle";
    }

    private String descriptionOf(GetSharedPuzzleResponse puzzle, boolean korean) {
        if (puzzle.description() != null && !puzzle.description().isBlank()) {
            return puzzle.description();
        }
        boolean blackWins = "BLACK".equals(puzzle.winColor());
        if (korean) {
            return (blackWins ? "흑선승" : "백선승") + " · 깊이 " + puzzle.depth() + " · Renzzle에서 풀어 보세요";
        }
        return (blackWins ? "Black to win" : "White to win") + " · Depth " + puzzle.depth() + " · Solve it on Renzzle";
    }

    private String findStoreUrl(String tag) {
        return appInfoService.findValue(tag)
                .filter(url -> url.startsWith("https://"))
                .orElse(null);
    }

    private String appStoreIdOf(String iosStoreUrl) {
        if (iosStoreUrl == null) {
            return null;
        }
        Matcher matcher = APP_STORE_ID.matcher(iosStoreUrl);
        return matcher.find() ? matcher.group(1) : null;
    }
}
