package com.renzzle.backend.domain.puzzle.cache.web;

import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.security.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Pages are admin-only, but the /api/puzzle/cache REST API is open to any authenticated user
@Controller
public class PuzzleCachePageController {

    private static final String USER_EMAIL_ATTRIBUTE = "userEmail";

    @GetMapping("/admin/puzzle-cache")
    public String puzzleCachePage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            Model model
    ) {
        if (userDetails != null) {
            model.addAttribute(USER_EMAIL_ATTRIBUTE, userDetails.getUser().getEmail());
        }
        model.addAttribute("langCodeNames", LangCode.LangCodeName.values());
        return "admin/puzzle-cache";
    }

    @GetMapping("/admin/puzzle-cache/board")
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
            model.addAttribute(USER_EMAIL_ATTRIBUTE, userDetails.getUser().getEmail());
        }
        return "admin/puzzle-cache-board";
    }

    @GetMapping("/admin/puzzle-cache/training-pack")
    public String puzzleCacheTrainingPack(
            @RequestParam("packId") Long packId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            Model model
    ) {
        model.addAttribute("packId", packId);
        if (userDetails != null) {
            model.addAttribute(USER_EMAIL_ATTRIBUTE, userDetails.getUser().getEmail());
        }
        return "admin/puzzle-cache-training-pack";
    }
}
