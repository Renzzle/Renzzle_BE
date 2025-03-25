package com.renzzle.backend.domain.puzzle.community.api;

import lombok.RequiredArgsConstructor;
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
