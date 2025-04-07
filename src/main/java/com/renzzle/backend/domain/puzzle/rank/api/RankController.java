package com.renzzle.backend.domain.puzzle.rank.api;

import com.renzzle.backend.domain.puzzle.rank.api.request.RankResultRequest;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankEndResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankResultResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.RankStartResponse;
import com.renzzle.backend.domain.puzzle.rank.domain.RankSessionData;
import com.renzzle.backend.domain.puzzle.rank.service.RankService;
import com.renzzle.backend.global.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
                                                               RankResultRequest request) {
        return ResponseEntity.ok(rankService.resultRankGame(user.getUser(), request));
    }

    @Operation(summary = "Start rank game", description = "Start rank game for 5 minutes")
    @PostMapping("/game/end")
    public ResponseEntity<RankEndResponse> endRankGame(@AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(rankService.endRankGame(user.getUser()));
    }
}
