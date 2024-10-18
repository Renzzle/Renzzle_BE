package com.renzzle.backend.domain.puzzle.api.response;

import lombok.Builder;

@Builder
public record GetLessonProgressResponse(
        double progress
) { }
