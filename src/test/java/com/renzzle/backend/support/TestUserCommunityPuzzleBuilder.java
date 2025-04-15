package com.renzzle.backend.support;

import com.renzzle.backend.domain.puzzle.community.dao.UserCommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.community.domain.UserCommunityPuzzle;
import com.renzzle.backend.domain.user.domain.UserEntity;

import java.time.Instant;

public class TestUserCommunityPuzzleBuilder {

    private final UserEntity user;
    private final CommunityPuzzle puzzle;

    private boolean isSolved = false;
    private Instant solvedAt = null;
    private boolean isLiked = false;
    private boolean isDisliked = false;
    private Instant likedAt = null;

    public TestUserCommunityPuzzleBuilder(UserEntity user, CommunityPuzzle puzzle) {
        this.user = user;
        this.puzzle = puzzle;
    }

    public static TestUserCommunityPuzzleBuilder builder(UserEntity user, CommunityPuzzle puzzle) {
        return new TestUserCommunityPuzzleBuilder(user, puzzle);
    }

    public TestUserCommunityPuzzleBuilder withSolved(boolean isSolved) {
        this.isSolved = isSolved;
        return this;
    }

    public TestUserCommunityPuzzleBuilder withSolvedAt(Instant solvedAt) {
        this.solvedAt = solvedAt;
        return this;
    }

    public TestUserCommunityPuzzleBuilder withLiked(boolean isLiked) {
        this.isLiked = isLiked;
        return this;
    }

    public TestUserCommunityPuzzleBuilder withDisliked(boolean isDisliked) {
        this.isDisliked = isDisliked;
        return this;
    }

    public TestUserCommunityPuzzleBuilder withLikedAt(Instant likedAt) {
        this.likedAt = likedAt;
        return this;
    }

    public UserCommunityPuzzle build() {
        return UserCommunityPuzzle.builder()
                .user(user)
                .puzzle(puzzle)
                .isSolved(isSolved)
                .solvedAt(solvedAt)
                .isLiked(isLiked)
                .isDisliked(isDisliked)
                .likedAt(likedAt)
                .build();
    }

    public UserCommunityPuzzle save(UserCommunityPuzzleRepository repository) {
        return repository.save(build());
    }

}
