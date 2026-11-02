package com.renzzle.backend.domain.puzzle.training.dao.projection;

import java.time.Instant;

public interface PackLastSolvedProjection {

    Long getPackId();
    Instant getLastSolvedAt();

}
