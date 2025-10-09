package com.renzzle.backend.domain.puzzle.content.api;

import com.renzzle.backend.domain.puzzle.content.api.request.GetRecommendRequest;
import com.renzzle.backend.domain.puzzle.content.api.response.GetTrendPuzzlesResponse;
import com.renzzle.backend.domain.puzzle.content.api.response.getRecommendPackResponse;
import com.renzzle.backend.domain.puzzle.content.service.ContentService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
@Tag(name = "Content API", description = "Content for Home UI API")
public class ContentController {
    private final ContentService contentService;

    @Operation(summary = "Get recommend Pack", description = "Get recommend Pack")
    @GetMapping("/recommend")
    public ApiResponse<getRecommendPackResponse> getRecommendPack(
            @Valid @ModelAttribute GetRecommendRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        getRecommendPackResponse response = contentService.getRecommendedPack(request, user.getUser());
        return ApiUtils.success(response);
    }

    @Operation(summary = "Get Trend Puzzles", description = "Get 5 Trend Puzzles")
    @GetMapping("/trend")
    public ApiResponse<GetTrendPuzzlesResponse> getTrendPuzzles(
            @AuthenticationPrincipal UserDetailsImpl user
    ){
        return ApiUtils.success(contentService.getTrendCommunityPuzzles(user.getUser()));
    }
}
