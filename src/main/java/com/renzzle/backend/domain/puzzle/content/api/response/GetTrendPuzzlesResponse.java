package com.renzzle.backend.domain.puzzle.content.api.response;

import java.util.List;

public record GetTrendPuzzlesResponse(
        List<getTrendPuzzleResponse> puzzles
) {
}
