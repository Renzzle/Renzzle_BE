package com.renzzle.backend.domain.puzzle.rank.api;

import com.renzzle.backend.domain.puzzle.rank.api.request.RankResultRequest;
import com.renzzle.backend.domain.puzzle.rank.api.response.*;
import com.renzzle.backend.domain.puzzle.rank.service.RankService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
@Tag(name = "Rank API", description = "Rank Game API")
public class RankController {

    private final RankService rankService;

    @Operation(summary = "Start rank game", description = "Start rank game for 5 minutes")
    @PostMapping("/game/start")
    public ApiResponse<RankStartResponse> startRankGame(@AuthenticationPrincipal UserDetailsImpl user) {
        return ApiUtils.success(rankService.startRankGame(user.getUser()));
    }

    @Operation(summary = "Send rank game puzzle result", description = "Send rank game puzzle result and get next puzzle info")
    @PostMapping("/game/result")
    public ApiResponse<RankResultResponse> resultRankGame(@AuthenticationPrincipal UserDetailsImpl user,
                                                             @Valid @RequestBody RankResultRequest request) {
        return ApiUtils.success(rankService.resultRankGame(user.getUser(), request));
    }

    @Operation(summary = "Start rank game", description = "Start rank game for 5 minutes")
    @PostMapping("/game/end")
    public ApiResponse<RankEndResponse> endRankGame(@AuthenticationPrincipal UserDetailsImpl user) {
        return ApiUtils.success(rankService.endRankGame(user.getUser()));
    }

    @Operation(summary = "Get rank game archive", description = "Get user past rank game archive")
    @GetMapping("/game/archive")
    public ApiResponse<List<RankArchive>> getRankGameArchive(@AuthenticationPrincipal UserDetailsImpl user) {
        return ApiUtils.success(rankService.getRankArchive(user.getUser()));
    }

    @Operation(summary = "get rating ranking", description = "get TOP 100 rating ranking and user ranking")
    @GetMapping("rating")
    public ApiResponse<GetRatingRankingResponse> getRatingRanking(@AuthenticationPrincipal UserDetailsImpl user) {
        return ApiUtils.success(rankService.getRatingRanking(user.getUser()));
    }

    @Operation(summary = "get community puzzler ranking", description = "get TOP 100 community puzzler ranking and user community puzzler ranking")
    @GetMapping("community")
    public ApiResponse<GetPuzzlerRankingResponse> getPuzzlerRanking(@AuthenticationPrincipal UserDetailsImpl user) {
        return ApiUtils.success(rankService.getPuzzlerRanking(user.getUser()));
    }
}
