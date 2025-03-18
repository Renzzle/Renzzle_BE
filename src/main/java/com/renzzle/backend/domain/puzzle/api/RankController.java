package com.renzzle.backend.domain.puzzle.api;

import com.renzzle.backend.domain.puzzle.api.request.PurchaseTrainingPackRequest;
import com.renzzle.backend.domain.puzzle.api.response.RankStartResponse;
import com.renzzle.backend.domain.puzzle.service.RankService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
public class RankController {

//    private final RankService rankGameService;
//
//    @Operation(summary = "Start rank game", description = "Start rank game for 5 minutes")
//    @PostMapping("/game/start")
//    public ApiResponse<RankStartResponse> startRankGame(
//            @AuthenticationPrincipal UserDetailsImpl user
//    ) {
//        RankStartResponse response = rankGameService.RankStart(user.getUser());
//        return ApiUtils.success(response);
//    }
//
//    @Operation(summary = "Send rank game puzzle result", description = "Send rank game puzzle result and get next puzzle info")
//    @PostMapping("/game/result")
//    public ApiResponse<RankStartResponse> resultRankGame(
//            @Valid @RequestBody PurchaseTrainingPackRequest request,
//            @AuthenticationPrincipal UserDetailsImpl user
//    ) {
//        RankStartResponse response = rankGameService.RankStart(user.getUser());
//        return ApiUtils.success(response);
//    }

}
