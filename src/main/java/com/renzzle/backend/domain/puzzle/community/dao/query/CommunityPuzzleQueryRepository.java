package com.renzzle.backend.domain.puzzle.community.dao.query;

import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;

import java.util.List;

public interface CommunityPuzzleQueryRepository {

    List<CommunityPuzzle> searchCommunityPuzzles(GetCommunityPuzzleRequest request, Long userId);

    /**
     * 캐시 입력용: 작성자 닉네임 완전 일치(비어 있으면 미적용), 승리 색, 깊이 구간, id 커서 페이지네이션.
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
