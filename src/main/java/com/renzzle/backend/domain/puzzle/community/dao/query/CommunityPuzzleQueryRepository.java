package com.renzzle.backend.domain.puzzle.community.dao.query;

import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.user.domain.UserEntity;

import java.util.List;

public interface CommunityPuzzleQueryRepository {

    // seed fixes the RECOMMEND shuffle; other sorts ignore it
    List<CommunityPuzzle> searchCommunityPuzzles(GetCommunityPuzzleRequest request, UserEntity user, long seed);

    List<CommunityPuzzle> searchCommunityPuzzlesForCache(
            String authorNicknameExact,
            String stone,
            Integer depthMin,
            Integer depthMax,
            Long cursorId,
            int size
    );
}
