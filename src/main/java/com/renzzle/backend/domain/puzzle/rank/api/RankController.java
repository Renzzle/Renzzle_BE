package com.renzzle.backend.domain.puzzle.rank.api;

import com.renzzle.backend.domain.puzzle.rank.api.request.RankResultRequest;
import com.renzzle.backend.domain.puzzle.rank.api.response.*;
import com.renzzle.backend.domain.puzzle.rank.domain.RankSessionData;
import com.renzzle.backend.domain.puzzle.rank.service.RankService;
import com.renzzle.backend.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    @Operation(summary = "Start rank game", description = "Start rank game for 5 minutes")
    @PostMapping("/game/start")
    public ResponseEntity<RankStartResponse> startRankGame(@AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(rankService.startRankGame(user.getUser()));
    }

    @Operation(summary = "Send rank game puzzle result", description = "Send rank game puzzle result and get next puzzle info")
    @PostMapping("/game/result")
    public ResponseEntity<RankResultResponse> resultRankGame(@AuthenticationPrincipal UserDetailsImpl user,
                                                             @Valid @RequestBody RankResultRequest request) {
        return ResponseEntity.ok(rankService.resultRankGame(user.getUser(), request));
    }

    @Operation(summary = "Start rank game", description = "Start rank game for 5 minutes")
    @PostMapping("/game/end")
    public ResponseEntity<RankEndResponse> endRankGame(@AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(rankService.endRankGame(user.getUser()));
    }

    @Operation(summary = "Get rank game archive", description = "Get user past rank game archive")
    @GetMapping("/game/archive")
    public ResponseEntity<List<RankArchive>> getRankGameArchive(@AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(rankService.getRankArchive(user.getUser()));
    }

    @Operation(summary = "get rating ranking", description = "get TOP 100 rating ranking and user ranking")
    @GetMapping("rating")
    public ResponseEntity<GetRankingResponse> getRanking(@AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(rankService.getRanking(user.getUser()));
    }
}
