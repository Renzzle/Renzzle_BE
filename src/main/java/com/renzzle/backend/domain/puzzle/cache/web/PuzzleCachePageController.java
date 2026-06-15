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
 * HTML for admins to enter the puzzle cache from a browser.
 * {@code GET /puzzle-cache} and {@code GET /puzzle-cache/board} require the ADMIN authority.
 * The cache REST API ({@code /api/puzzle/cache/**}) can also be called by regular authenticated users.
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
