package com.renzzle.backend.domain.puzzle.community.api.response;

import java.util.List;

public record GetCommunityPuzzlePageForAdminResponse(
        List<GetCommunityPuzzleForAdminResponse> puzzles,
        int page,
        int size,
        long totalCount,
        int totalPages
) { }
