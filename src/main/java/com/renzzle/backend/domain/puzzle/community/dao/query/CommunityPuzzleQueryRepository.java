package com.renzzle.backend.domain.puzzle.community.dao.query;

import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.user.domain.UserEntity;

import java.util.List;

public interface CommunityPuzzleQueryRepository {

    /**
     * @param seed fixes the RECOMMEND shuffle order; unused by the other sorts.
     */
    List<CommunityPuzzle> searchCommunityPuzzles(GetCommunityPuzzleRequest request, UserEntity user, long seed);

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
