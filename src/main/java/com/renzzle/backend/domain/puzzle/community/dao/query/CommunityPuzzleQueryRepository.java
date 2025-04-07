package com.renzzle.backend.domain.puzzle.community.dao.query;

import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;

import java.util.List;

public interface CommunityPuzzleQueryRepository {

    List<CommunityPuzzle> searchCommunityPuzzles(GetCommunityPuzzleRequest request, Long userId);

}
