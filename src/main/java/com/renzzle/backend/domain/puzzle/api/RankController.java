package com.renzzle.backend.domain.puzzle.api;

import com.renzzle.backend.domain.puzzle.api.response.RankStartResponse;
import com.renzzle.backend.domain.puzzle.service.RankService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankGameService;

    @PostMapping("/game/start")
    public ApiResponse<RankStartResponse> startRankGame(
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        RankStartResponse response = rankGameService.RankStart(user.getUser());
        return ApiUtils.success(response);
    }






}
