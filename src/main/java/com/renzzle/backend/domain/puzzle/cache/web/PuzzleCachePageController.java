package com.renzzle.backend.domain.puzzle.cache.web;

import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.security.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 어드민이 브라우저에서 퍼즐 캐시를 입력하기 위한 HTML.
 * {@code GET /puzzle-cache}, {@code GET /puzzle-cache/board}는 ADMIN 권한 필요.
 * 캐시 REST API({@code /api/puzzle/cache/**})는 일반 인증 사용자도 호출 가능.
 */
@Controller
public class PuzzleCachePageController {

    @GetMapping("/puzzle-cache")
    public String puzzleCachePage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            Model model
    ) {
        if (userDetails != null) {
            model.addAttribute("userEmail", userDetails.getUser().getEmail());
        }
        model.addAttribute("langCodeNames", LangCode.LangCodeName.values());
        return "puzzle-cache";
    }

    @GetMapping("/puzzle-cache/board")
    public String puzzleCacheBoard(
            @RequestParam(value = "puzzleType", defaultValue = "TRAINING") PuzzleType puzzleType,
            @RequestParam("puzzleId") Long puzzleId,
            @RequestParam(value = "packId", required = false) Long packId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            Model model
    ) {
        model.addAttribute("puzzleType", puzzleType.name());
        model.addAttribute("puzzleId", puzzleId);
        model.addAttribute("packId", packId);
        if (userDetails != null) {
            model.addAttribute("userEmail", userDetails.getUser().getEmail());
        }
        return "puzzle-cache-board";
    }
}
