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

import java.util.List;
import java.util.Optional;

import static com.renzzle.backend.support.TestTime.FIXED_INSTANT;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithInitContainers
@Import({TrainingPuzzleSeeder.class, CommunityPuzzleSeeder.class, PackSeeder.class})
class RankRepositoryTest {

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

    @Test
    void saveLatestRankPuzzle_WhenTrainingPuzzleGiven_ThenSaveSuccessfully() {
        // Create user
        UserEntity user = userRepository.save(TestUserFactory.createTestUser("seeder-user", 1500));

        // Create puzzles via seeder
        trainingPuzzleSeeder.seedPuzzle(1, "a1a2", "a3", 3, 1400, "BLACK");
        communityPuzzleSeeder.seedPuzzle("b1b2", "b3", 4, 1450, "WHITE", user);

        // Look up the puzzle directly and create a ranking record
        TrainingPuzzle training = trainingPuzzleRepository.findAll().get(0);
        LatestRankPuzzle latest = LatestRankPuzzle.builder()
                .user(user)
                .boardStatus(training.getBoardStatus())
                .answer(training.getAnswer())
                .winColor(training.getWinColor())
                .assignedAt(FIXED_INSTANT)
                .isSolved(false)
                .build();
        latestRankPuzzleRepository.save(latest);

        // Verify
        List<LatestRankPuzzle> all = latestRankPuzzleRepository.findAllByUser(user);
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getBoardStatus()).isEqualTo("a1a2");
    }

    @Test
    void saveLatestRankPuzzle_WhenCommunityPuzzleIsGiven_ThenSaveSuccessfully() {
        // Given
        UserEntity user = userRepository.save(TestUserFactory.createTestUser("author", 1500));

        // Create community puzzle via seeder
        communityPuzzleSeeder.seedPuzzle(
                "b1b2", "b3", 4, 1450.0, "WHITE", user
        );
        CommunityPuzzle community = communityPuzzleRepository.findAll().get(0);

        // Save ranking puzzle
        LatestRankPuzzle latest = latestRankPuzzleRepository.save(
                LatestRankPuzzle.builder()
                        .user(user)
                        .boardStatus(community.getBoardStatus())
                        .answer(community.getAnswer())
                        .winColor(community.getWinColor())
                        .assignedAt(FIXED_INSTANT)
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
