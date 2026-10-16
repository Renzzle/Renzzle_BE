package com.renzzle.backend.domain.admin.api;

import com.jayway.jsonpath.JsonPath;
import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.domain.Admin;
import com.renzzle.backend.domain.auth.domain.AdminPermission;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.puzzle.cache.dao.PuzzleCacheRepository;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.security.AppKeyAuthenticationFilter;
import com.renzzle.backend.support.TestCommunityPuzzleBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
class AdminCommunityPuzzleListTest {

    private static final String ENDPOINT = "/admin/community/puzzle-manage";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private CommunityPuzzleRepository communityPuzzleRepository;
    @Autowired private PuzzleCacheRepository puzzleCacheRepository;

    private final List<UserEntity> createdUsers = new ArrayList<>();
    private final List<Long> unverifiedIds = new ArrayList<>();
    private final List<Long> verifiedIds = new ArrayList<>();
    private UserEntity communityAdmin;

    @BeforeEach
    void setUp() {
        UserEntity author = createUser();
        for (int i = 0; i < 3; i++) {
            unverifiedIds.add(savePuzzle(author, false));
        }
        for (int i = 0; i < 2; i++) {
            verifiedIds.add(savePuzzle(author, true));
        }
        communityAdmin = createAdmin(AdminPermission.COMMUNITY_MANAGE);
    }

    @AfterEach
    void tearDown() {
        // Verifying seeds the solution line, which outlives the puzzle rows
        unverifiedIds.forEach(id -> puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(PuzzleType.COMMUNITY, id)
                .ifPresent(puzzleCacheRepository::delete));
        communityPuzzleRepository.deleteAllById(unverifiedIds);
        communityPuzzleRepository.deleteAllById(verifiedIds);
        userRepository.deleteAll(createdUsers);
    }

    @Test
    void list_WhenUnverifiedRequested_ThenReturnsOnlyUnverifiedNewestFirst() throws Exception {
        String body = mockMvc.perform(withToken(get(ENDPOINT).param("verified", "false").param("size", "50"), communityAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Boolean> verifiedFlags = JsonPath.read(body, "$.response.puzzles[*].isVerified");
        List<Number> ids = JsonPath.read(body, "$.response.puzzles[*].id");
        List<Long> returnedIds = ids.stream().map(Number::longValue).toList();

        assertThat(verifiedFlags).isNotEmpty().containsOnly(false);
        assertThat(returnedIds).doesNotContainAnyElementsOf(verifiedIds);
        assertThat(returnedIds.stream().filter(unverifiedIds::contains).toList())
                .containsExactly(unverifiedIds.get(2), unverifiedIds.get(1), unverifiedIds.get(0));
    }

    @Test
    void list_WhenPageSizeIsSmall_ThenSplitsIntoPages() throws Exception {
        String body = mockMvc.perform(withToken(get(ENDPOINT).param("verified", "false").param("size", "2"), communityAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.page").value(0))
                .andExpect(jsonPath("$.response.size").value(2))
                .andExpect(jsonPath("$.response.puzzles.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        long totalCount = JsonPath.<Number>read(body, "$.response.totalCount").longValue();
        int totalPages = JsonPath.read(body, "$.response.totalPages");
        assertThat(totalCount).isGreaterThanOrEqualTo(unverifiedIds.size());
        assertThat(totalPages).isEqualTo((int) Math.ceil(totalCount / 2.0));
    }

    @Test
    void list_WhenNoFilter_ThenIncludesVerifiedAndUnverified() throws Exception {
        String body = mockMvc.perform(withToken(get(ENDPOINT).param("size", "50"), communityAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Number> ids = JsonPath.read(body, "$.response.puzzles[*].id");
        assertThat(ids.stream().map(Number::longValue).toList())
                .containsAll(unverifiedIds)
                .containsAll(verifiedIds);
    }

    @Test
    void list_WhenAdminLacksCommunityPermission_ThenDenied() throws Exception {
        mockMvc.perform(withToken(get(ENDPOINT), createAdmin(AdminPermission.PACK_MANAGE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorResponse.code").value("A4031"));
    }

    @Test
    void list_WhenSizeOutOfRange_ThenRejected() throws Exception {
        mockMvc.perform(withToken(get(ENDPOINT).param("size", "0"), communityAdmin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(withToken(get(ENDPOINT).param("size", "51"), communityAdmin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verify_WhenCorrectedAnswerSent_ThenSavesItWithTheVerification() throws Exception {
        Long puzzleId = unverifiedIds.get(0);

        mockMvc.perform(withToken(patch(ENDPOINT + "/" + puzzleId + "/verification"), communityAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isVerified\":true,\"answer\":\"g7i9\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.isVerified").value(true))
                .andExpect(jsonPath("$.response.answer").value("g7i9"))
                .andExpect(jsonPath("$.response.depth").value(2));

        assertThat(communityPuzzleRepository.findById(puzzleId).orElseThrow().getAnswer()).isEqualTo("g7i9");
    }

    @Test
    void verify_WhenCorrectedAnswerLandsOnAStone_ThenRejectsAndKeepsThePuzzleUnverified() throws Exception {
        Long puzzleId = unverifiedIds.get(0);

        mockMvc.perform(withToken(patch(ENDPOINT + "/" + puzzleId + "/verification"), communityAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isVerified\":true,\"answer\":\"h8i9\"}"))
                .andExpect(status().isBadRequest());

        CommunityPuzzle puzzle = communityPuzzleRepository.findById(puzzleId).orElseThrow();
        assertThat(puzzle.getAnswer()).isEqualTo("i9");
        assertThat(puzzle.getIsVerified()).isFalse();
    }

    private Long savePuzzle(UserEntity author, boolean verified) {
        CommunityPuzzle puzzle = TestCommunityPuzzleBuilder.builder(author)
                .withBoardStatus("h8h9")
                .withAnswer("i9")
                .withVerified(verified)
                .save(communityPuzzleRepository);
        return puzzle.getId();
    }

    private UserEntity createUser() {
        UserEntity user = userRepository.save(
                TestUserFactory.createTestUser("list-" + UUID.randomUUID().toString().substring(0, 8), 1000));
        createdUsers.add(user);
        return user;
    }

    private UserEntity createAdmin(AdminPermission permission) {
        UserEntity user = createUser();
        adminRepository.save(Admin.builder().user(user).permissions(EnumSet.of(permission)).build());
        return user;
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, UserEntity user) {
        return request
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(user.getId(), "test-session"))
                .header(AppKeyAuthenticationFilter.APP_KEY_HEADER, "test-app-key");
    }
}
