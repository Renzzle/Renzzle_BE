package com.renzzle.backend.domain.puzzle.community.dao;

import com.renzzle.backend.config.DataJpaTestWithInitContainers;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.global.init.DataInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DataJpaTestWithInitContainers
public class CommunityPuzzleRepositoryTest {

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityPuzzleRepository communityPuzzleRepository;

    @Autowired
    private UserCommunityPuzzleRepository userCommunityPuzzleRepository;

    @Test
    public void searchCommunityPuzzles_WhenVariousConditions_ThenReturnsExpectedResults() {
        // Given

    }

}
