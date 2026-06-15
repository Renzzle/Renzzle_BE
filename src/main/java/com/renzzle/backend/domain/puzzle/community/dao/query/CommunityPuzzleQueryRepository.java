package com.renzzle.backend.domain.puzzle.community.dao.query;

import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;

import java.util.List;

public interface CommunityPuzzleQueryRepository {

    List<CommunityPuzzle> searchCommunityPuzzles(GetCommunityPuzzleRequest request, Long userId);

    /**
     * For cache entry: exact author-nickname match (not applied if empty), win color, depth range, and id-cursor pagination.
     */
    List<CommunityPuzzle> searchCommunityPuzzlesForCache(
            String authorNicknameExact,
            String stone,
            Integer depthMin,
            Integer depthMax,
            Long cursorId,
            int size
    );
}
