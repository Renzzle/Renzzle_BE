package com.renzzle.backend.support;

import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.Status;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.UUID;

import static com.renzzle.backend.global.common.constant.TimeConstant.CONST_FUTURE_INSTANT;

public class TestCommunityPuzzleBuilder {

    private static Long idx = 0L;

    private final UserEntity user;

    private Long id = null;
    private String boardStatus = "default_status";
    private String boardKey = UUID.randomUUID().toString();
    private String answer = "default_answer";
    private Integer depth = 1;
    private Boolean isVerified = true;
    private Double rating = 1000.0;
    private int likeCount = 0;
    private int dislikeCount = 0;
    private int view = 0;
    private String description = null;
    private Instant createdAt = null;
    private Instant updatedAt = null;
    private Instant deletedAt = null;
    private Status status = null;
    private WinColor winColor = WinColor.getWinColor("BLACK");

    public TestCommunityPuzzleBuilder(UserEntity user) {
        this.user = user;
    }

    public static TestCommunityPuzzleBuilder builder(UserEntity user) {
        return new TestCommunityPuzzleBuilder(user);
    }

    public TestCommunityPuzzleBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public TestCommunityPuzzleBuilder withBoardStatus(String boardStatus) {
        this.boardStatus = boardStatus;
        return this;
    }

    public TestCommunityPuzzleBuilder withBoardKey(String boardKey) {
        this.boardKey = boardKey;
        return this;
    }

    public TestCommunityPuzzleBuilder withAnswer(String answer) {
        this.answer = answer;
        return this;
    }

    public TestCommunityPuzzleBuilder withDepth(int depth) {
        this.depth = depth;
        return this;
    }

    public TestCommunityPuzzleBuilder withVerified(boolean verified) {
        this.isVerified = verified;
        return this;
    }

    public TestCommunityPuzzleBuilder withRating(Double rating) {
        this.rating = rating;
        return this;
    }

    public TestCommunityPuzzleBuilder withLikeCount(int likeCount) {
        this.likeCount = likeCount;
        return this;
    }

    public TestCommunityPuzzleBuilder withDislikeCount(int dislikeCount) {
        this.dislikeCount = dislikeCount;
        return this;
    }

    public TestCommunityPuzzleBuilder withView(int view) {
        this.view = view;
        return this;
    }

    public TestCommunityPuzzleBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public TestCommunityPuzzleBuilder withCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public TestCommunityPuzzleBuilder withUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public TestCommunityPuzzleBuilder withDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
        return this;
    }

    public TestCommunityPuzzleBuilder withStatus(Status status) {
        this.status = status;
        return this;
    }

    public TestCommunityPuzzleBuilder withColor(WinColor color) {
        this.winColor = color;
        return this;
    }

    public CommunityPuzzle build() {
        return CommunityPuzzle.builder()
                .id(id)
                .boardStatus(boardStatus)
                .boardKey(boardKey)
                .answer(answer)
                .depth(depth)
                .isVerified(isVerified)
                .rating(rating)
                .likeCount(likeCount)
                .dislikeCount(dislikeCount)
                .view(view)
                .description(description)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .status(status)
                .user(user)
                .winColor(winColor)
                .build();
    }

    public CommunityPuzzle save(CommunityPuzzleRepository repository) {
        if (repository == null || Mockito.mockingDetails(repository).isMock()) {
            id = (id == null) ? ++idx : id;
            status = (status == null) ? Status.getDefaultStatus() : status;
            createdAt = (createdAt == null) ? Instant.now() : createdAt;
            updatedAt = (updatedAt == null) ? Instant.now() : updatedAt;
            deletedAt = (deletedAt == null) ? CONST_FUTURE_INSTANT : deletedAt;
            return build();
        } else {
            CommunityPuzzle puzzle = repository.save(build());
            idx = puzzle.getId();
            return puzzle;
        }
    }

}
