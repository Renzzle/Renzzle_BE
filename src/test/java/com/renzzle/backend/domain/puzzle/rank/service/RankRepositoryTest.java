package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.rank.dao.LatestRankPuzzleRepository;
import com.renzzle.backend.domain.puzzle.rank.domain.LatestRankPuzzle;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.puzzle.rank.util.CommunityPuzzleSeeder;
import com.renzzle.backend.domain.puzzle.rank.util.PackSeeder;
import com.renzzle.backend.domain.puzzle.rank.util.TrainingPuzzleSeeder;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.support.DataJpaTestWithInitContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithInitContainers
@Import({TrainingPuzzleSeeder.class, CommunityPuzzleSeeder.class, PackSeeder.class})
public class RankRepositoryTest {

    @Autowired
    private TrainingPuzzleSeeder trainingPuzzleSeeder;

    @Autowired
    private CommunityPuzzleSeeder communityPuzzleSeeder;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrainingPuzzleRepository trainingPuzzleRepository;

    @Autowired
    private CommunityPuzzleRepository communityPuzzleRepository;

    @Autowired
    private LatestRankPuzzleRepository latestRankPuzzleRepository;

//    @Autowired
//    private EntityManager em;
//    @Autowired
//    private Clock clock;

    @Test
    void saveLatestRankPuzzle_WithTrainingPuzzle_ShouldPersistCorrectly() {
        // 사용자 생성
        UserEntity user = userRepository.save(TestUserFactory.createTestUser("seeder-user", 1500));

        // 퍼즐 생성 via seeder
        trainingPuzzleSeeder.seedPuzzle(1, "a1a2", "a3", 3, 1400, "BLACK");
        communityPuzzleSeeder.seedPuzzle("b1b2", "b3", 4, 1450, "WHITE", user);

        // 퍼즐 직접 조회해서 랭킹 기록 생성
        TrainingPuzzle training = trainingPuzzleRepository.findAll().get(0);
        LatestRankPuzzle latest = LatestRankPuzzle.builder()
                .user(user)
                .boardStatus(training.getBoardStatus())
                .answer(training.getAnswer())
                .winColor(training.getWinColor())
                .assignedAt(Instant.now())
                .isSolved(false)
                .build();
        latestRankPuzzleRepository.save(latest);

        // 검증
        List<LatestRankPuzzle> all = latestRankPuzzleRepository.findAllByUser(user);
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getBoardStatus()).isEqualTo("a1a2");
    }

    @Test
    void saveLatestRankPuzzle_WithCommunityPuzzle_ShouldPersistCorrectly() {
        // Given
        UserEntity user = userRepository.save(TestUserFactory.createTestUser("author", 1500));

        // 시더를 통해 커뮤니티 퍼즐 생성
        communityPuzzleSeeder.seedPuzzle(
                "b1b2", "b3", 4, 1450.0, "WHITE", user
        );
        CommunityPuzzle community = communityPuzzleRepository.findAll().get(0);

        // 랭킹 퍼즐 저장
        LatestRankPuzzle latest = latestRankPuzzleRepository.save(
                LatestRankPuzzle.builder()
                        .user(user)
                        .boardStatus(community.getBoardStatus())
                        .answer(community.getAnswer())
                        .winColor(community.getWinColor())
                        .assignedAt(Instant.now())
                        .isSolved(false)
                        .build()
        );

        // When
        Optional<LatestRankPuzzle> saved = latestRankPuzzleRepository.findTopByUserOrderByAssignedAtDesc(user);

        // Then
        assertThat(saved).isPresent();
        assertThat(saved.get().getBoardStatus()).isEqualTo("b1b2");
        assertThat(saved.get().getIsSolved()).isFalse();
    }
}
