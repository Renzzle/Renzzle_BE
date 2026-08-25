package com.renzzle.backend.domain.puzzle.cache.api.response;

/**
 * A cached user move paired with the AI reply, both as board positions such as {@code "h8"}.
 * The client keys its local lookup by {@code userMove}, so no hashing is needed on the client.
 */
public record NextMoveCandidateResponse(
        String userMove,
        String aiResponse
) { }
