package com.renzzle.backend.domain.puzzle.community.dao.projection;

public interface AuthorStatsProjection {

    Long getUserId();
    long getPuzzleCount();
    long getLikeSum();
    long getDislikeSum();

}
