package com.renzzle.backend.domain.puzzle.content.api.response;

import com.renzzle.backend.domain.puzzle.community.api.response.GetCommunityPuzzlesResponse;

import java.util.List;

public record GetTrendPuzzlesResponse(
        List<GetCommunityPuzzlesResponse> puzzles
) {
}
