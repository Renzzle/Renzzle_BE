package com.renzzle.backend.domain.puzzle.cache.api.response;

// Positions like "h8"; the client keys its lookup by userMove
public record NextMoveCandidateResponse(
        String userMove,
        String aiResponse
) { }
