package com.renzzle.backend.domain.puzzle.community.dao.projection;

import java.time.Instant;

public interface CommunityBoardKeyProjection {

    Long getId();
    String getBoardStatus();
    String getBoardKey();
    String getStatus();
    Instant getDeletedAt();

}
