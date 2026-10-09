package com.renzzle.backend.domain.admin.api;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.domain.Admin;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.puzzle.community.service.CommunityService;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.puzzle.shared.dto.AnswerKeyRecalculationResult;
import com.renzzle.backend.domain.puzzle.shared.dto.BoardKeyRecalculationResult;
import com.renzzle.backend.domain.puzzle.training.service.TrainingService;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.security.AppKeyAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
class AdminPuzzleRecalculationTest {

    private static final String RATING_ENDPOINT = "/admin/puzzle/rating/recalculate";
    private static final String BOARD_KEY_ENDPOINT = "/admin/puzzle/board-key/recalculate";
    private static final String ANSWER_KEY_ENDPOINT = "/admin/puzzle/answer-key/recalculate";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminRepository adminRepository;

    @MockBean private TrainingService trainingService;
    @MockBean private CommunityService communityService;

    private final List<UserEntity> createdUsers = new ArrayList<>();

    @AfterEach
    void tearDown() {
        adminRepository.deleteAll(adminRepository.findAll().stream()
                .filter(admin -> createdUsers.contains(admin.getUser()))
                .toList());
        userRepository.deleteAll(createdUsers);
        createdUsers.clear();
    }

    @Test
    void recalculatePuzzleRatings_WhenRegularUser_ThenForbidsAndRatesNothing() throws Exception {
        UserEntity user = createUser();

        mockMvc.perform(withToken(post(RATING_ENDPOINT), user))
                .andExpect(status().isForbidden());

        verify(trainingService, never()).recalculateUnrankedPuzzleRatings();
        verify(communityService, never()).recalculateUnrankedPuzzleRatings();
    }

    @Test
    void recalculatePuzzleRatings_WhenAdmin_ThenReturnsHowManyPuzzlesWereRerated() throws Exception {
        UserEntity user = createUser();
        adminRepository.save(Admin.builder().user(user).build());
        when(trainingService.recalculateUnrankedPuzzleRatings()).thenReturn(3);
        when(communityService.recalculateUnrankedPuzzleRatings()).thenReturn(5);

        mockMvc.perform(withToken(post(RATING_ENDPOINT), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.trainingPuzzleCount").value(3))
                .andExpect(jsonPath("$.response.communityPuzzleCount").value(5));
    }

    @Test
    void recalculateBoardKeys_WhenRegularUser_ThenForbidsAndRewritesNothing() throws Exception {
        UserEntity user = createUser();

        mockMvc.perform(withToken(post(BOARD_KEY_ENDPOINT), user))
                .andExpect(status().isForbidden());

        verify(trainingService, never()).recalculateBoardKeys();
        verify(communityService, never()).recalculateBoardKeys();
    }

    @Test
    void recalculateBoardKeys_WhenAdmin_ThenReturnsUpdatesAndDuplicates() throws Exception {
        UserEntity user = createUser();
        adminRepository.save(Admin.builder().user(user).build());
        when(trainingService.recalculateBoardKeys()).thenReturn(new BoardKeyRecalculationResult(3, List.of()));
        when(communityService.recalculateBoardKeys())
                .thenReturn(new BoardKeyRecalculationResult(5, List.of(List.of(7L, 9L))));

        mockMvc.perform(withToken(post(BOARD_KEY_ENDPOINT), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.training.updatedCount").value(3))
                .andExpect(jsonPath("$.response.training.duplicates").isEmpty())
                .andExpect(jsonPath("$.response.community.updatedCount").value(5))
                .andExpect(jsonPath("$.response.community.duplicates[0][0]").value(7))
                .andExpect(jsonPath("$.response.community.duplicates[0][1]").value(9));
    }

    @Test
    void recalculateAnswerKeys_WhenRegularUser_ThenForbidsAndRewritesNothing() throws Exception {
        UserEntity user = createUser();

        mockMvc.perform(withToken(post(ANSWER_KEY_ENDPOINT), user))
                .andExpect(status().isForbidden());

        verify(trainingService, never()).recalculateAnswerKeys();
        verify(communityService, never()).recalculateAnswerKeys();
    }

    @Test
    void recalculateAnswerKeys_WhenAdmin_ThenReturnsUpdatesAndInvalidPuzzles() throws Exception {
        UserEntity user = createUser();
        adminRepository.save(Admin.builder().user(user).build());
        when(trainingService.recalculateAnswerKeys()).thenReturn(new AnswerKeyRecalculationResult(3, List.of()));
        when(communityService.recalculateAnswerKeys()).thenReturn(new AnswerKeyRecalculationResult(5, List.of(7L)));

        mockMvc.perform(withToken(post(ANSWER_KEY_ENDPOINT), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.training.updatedCount").value(3))
                .andExpect(jsonPath("$.response.training.invalidIds").isEmpty())
                .andExpect(jsonPath("$.response.community.updatedCount").value(5))
                .andExpect(jsonPath("$.response.community.invalidIds[0]").value(7));
    }

    private UserEntity createUser() {
        UserEntity user = userRepository.save(
                TestUserFactory.createTestUser("rater-" + UUID.randomUUID().toString().substring(0, 8), 1000));
        createdUsers.add(user);
        return user;
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, UserEntity user) {
        return request
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(user.getId(), "test-session"))
                .header(AppKeyAuthenticationFilter.APP_KEY_HEADER, "test-app-key");
    }
}
