package com.renzzle.backend.domain.puzzle.rank.service;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.puzzle.rank.api.response.GetRankingResponse;
import com.renzzle.backend.domain.puzzle.rank.api.response.UserRankInfo;
import com.renzzle.backend.domain.puzzle.rank.dao.LatestRankPuzzleRepository;
import com.renzzle.backend.domain.puzzle.rank.domain.LatestRankPuzzle;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.puzzle.rank.util.PuzzleSeeder;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.util.ELOUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

//@SpringBootTest
//@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
@Transactional
@ExtendWith(MockitoExtension.class)
public class RankServiceTest {

    @InjectMocks
    private RankService rankService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private final String redisKey = "user:ranking";


    @BeforeEach
    void setup() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }



    @Test
    void getRanking_WhenUsersHaveSameRating_ThenAssignsSameRank() {
        // given
        UserEntity user = TestUserFactory.createTestUser("me", 1300.0);

        UserRankInfo info1 = UserRankInfo.builder().nickname("user1").rating(1500).rank(0).build();
        UserRankInfo info2 = UserRankInfo.builder().nickname("user2").rating(1500).rank(0).build();
        UserRankInfo myInfo = UserRankInfo.builder().nickname("me").rating(1300).rank(0).build();

        Set<ZSetOperations.TypedTuple<Object>> zset = new LinkedHashSet<>();
        zset.add(new DefaultTypedTuple<>(info1, 1500.0));
        zset.add(new DefaultTypedTuple<>(info2, 1500.0));
        zset.add(new DefaultTypedTuple<>(myInfo, 1300.0));

        when(zSetOperations.reverseRangeWithScores("user:ranking", 0, 99)).thenReturn(zset);
        when(zSetOperations.reverseRangeWithScores("user:ranking", 0, -1)).thenReturn(zset);
//        when(zSetOperations.reverseRank(eq("user:ranking"), eq(myInfo))).thenReturn(2L);

        // when
        GetRankingResponse response = rankService.getRanking(user);

        // then
        assertThat(response.top100()).hasSize(3);
        assertThat(response.top100().get(0).rank()).isEqualTo(1);
        assertThat(response.top100().get(1).rank()).isEqualTo(1);
        assertThat(response.top100().get(2).rank()).isEqualTo(3);
        assertThat(response.myRank().rank()).isEqualTo(3);
    }

}
