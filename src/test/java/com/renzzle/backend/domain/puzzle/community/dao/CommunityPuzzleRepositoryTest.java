package com.renzzle.backend.domain.puzzle.community.dao;

import com.renzzle.backend.config.DataJpaTestWithInitContainers;
import com.renzzle.backend.domain.puzzle.community.api.request.GetCommunityPuzzleRequest;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.community.domain.UserCommunityPuzzle;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.constant.SortOption;
import com.renzzle.backend.support.TestCommunityPuzzleBuilder;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.renzzle.backend.domain.puzzle.shared.domain.WinColor.getWinColor;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithInitContainers
public class CommunityPuzzleRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityPuzzleRepository communityPuzzleRepository;

    @Autowired
    private UserCommunityPuzzleRepository userCommunityPuzzleRepository;

    /*
    테스트 케이스 시나리오
    1. 동시에 모든 조건을 걸었을때 제대로 반환되는지 확인
    2. 아이디와 사이즈를 넣었을때 그 아이디 이후로 원하는 사이즈 만큼 반환되는지 확인 (커서 방식 동작 확인)
    3. 정렬이 제대로 되서 반환되는지 확인 (좋아요, 최신순)
    4. 흑 문제만 조회할때 정렬이 좋아요 순일때 동일한 좋아요 중간에서 잘릴때 그 아이디를 넣어서 요청하면 제대로 나머지들이 반환되는지 확인
     */

    @Test
    public void searchCommunityPuzzles_WhenVariousConditions_ThenReturnsExpectedResults() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder()
                .withNickname("test")
                .save(userRepository);
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(user)
                .withColor(getWinColor("BLACK"))
                .withDepth(7)
                .withVerified(true)
                .save(communityPuzzleRepository);
        userCommunityPuzzleRepository.save(
                UserCommunityPuzzle.builder()
                        .user(user)
                        .puzzle(puzzle)
                        .isSolved(true)
                        .build()
        );

        // When
        List<CommunityPuzzle> result = communityPuzzleRepository.searchCommunityPuzzles(
                new GetCommunityPuzzleRequest(null, null, null, "BLACK", true, 6, 7, true, "test"),
                user.getId()
        );

        // Then
        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(p -> {
                    assertThat(p.getId()).isEqualTo(puzzle.getId());
                    assertThat(p.getUser().getNickname()).isEqualTo(user.getNickname());
                    assertThat(p.getWinColor()).isEqualTo(getWinColor("BLACK"));
                    assertThat(p.getDepth()).isEqualTo(7);
                    assertThat(p.getIsVerified()).isTrue();
                });
    }

    @Test
    public void searchCommunityPuzzles_WhenCursorIdAndSizeProvided_ThenReturnsCorrectSubset() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);

        CommunityPuzzle puzzle1 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        CommunityPuzzle puzzle2 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);
        CommunityPuzzle puzzle3 = TestCommunityPuzzleBuilder.builder(user).save(communityPuzzleRepository);

        // When
        List<CommunityPuzzle> result = communityPuzzleRepository.searchCommunityPuzzles(
                new GetCommunityPuzzleRequest(puzzle3.getId(), 2, null, null, null, null, null, null, null),
                user.getId()
        );

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting("id")
                .containsExactly(puzzle2.getId(), puzzle1.getId());
    }

    @Test
    public void searchCommunityPuzzles_WhenSortIsLike_ThenOrderedByLikeCountDesc() {
        // Given
        UserEntity user = TestUserEntityBuilder.builder().save(userRepository);

        CommunityPuzzle puzzleA = TestCommunityPuzzleBuilder.builder(user)
                .withLikeCount(1)
                .save(communityPuzzleRepository);
        CommunityPuzzle puzzleB = TestCommunityPuzzleBuilder.builder(user)
                .withLikeCount(2)
                .save(communityPuzzleRepository);
        CommunityPuzzle puzzleC = TestCommunityPuzzleBuilder.builder(user)
                .withLikeCount(3)
                .save(communityPuzzleRepository);

        // When
        List<CommunityPuzzle> result = communityPuzzleRepository.searchCommunityPuzzles(
                new GetCommunityPuzzleRequest(null, 3, "LIKE", null, null, null, null, null, null),
                user.getId()
        );

        // Then
        assertThat(result)
                .extracting("id")
                .containsExactly(puzzleC.getId(), puzzleB.getId(), puzzleA.getId());
    }

}
