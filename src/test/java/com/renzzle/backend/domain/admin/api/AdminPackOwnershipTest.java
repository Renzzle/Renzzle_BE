package com.renzzle.backend.domain.admin.api;

import com.jayway.jsonpath.JsonPath;
import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.domain.Admin;
import com.renzzle.backend.domain.auth.domain.AdminPermission;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils;
import com.renzzle.backend.domain.puzzle.training.dao.PackRepository;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.security.AppKeyAuthenticationFilter;
import com.renzzle.backend.support.TestPackBuilder;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static com.renzzle.backend.domain.auth.domain.AdminPermission.AI_RESPONSE_MANAGE;
import static com.renzzle.backend.domain.auth.domain.AdminPermission.OPERATION_MANAGE;
import static com.renzzle.backend.domain.auth.domain.AdminPermission.PACK_MANAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
@Transactional
class AdminPackOwnershipTest {

    private static final String BOARD = "c3d3c4d4c5d5";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private PackRepository packRepository;
    @Autowired private TrainingPuzzleRepository trainingPuzzleRepository;

    private UserEntity owner;
    private UserEntity otherPackAdmin;
    private Long ownPackId;
    private Long legacyPackId;
    private Long ownPuzzleId;

    @BeforeEach
    void setUp() throws Exception {
        owner = createAdmin(PACK_MANAGE);
        otherPackAdmin = createAdmin(PACK_MANAGE);
        ownPackId = createPack(owner);
        legacyPackId = packRepository.save(TestPackBuilder.builder().build()).getId();
        ownPuzzleId = trainingPuzzleRepository.save(TrainingPuzzle.builder()
                .pack(packRepository.findById(ownPackId).orElseThrow())
                .trainingIndex(0)
                .boardStatus(BOARD)
                .boardKey(BoardUtils.makeBoardKey(BOARD))
                .answer("c6")
                .depth(1)
                .rating(1000)
                .winColor(WinColor.getWinColor("BLACK"))
                .build()).getId();
    }

    @Test
    void packAdmin_WhenCreatingPack_ThenBecomesItsCreator() {
        assertThat(packRepository.findById(ownPackId).orElseThrow().getCreatedBy()).isEqualTo(owner.getId());
    }

    @Test
    void packAdmin_WhenManagingOwnPack_ThenAllowed() throws Exception {
        mockMvc.perform(withToken(patch("/api/training/pack/" + ownPackId), owner).content(packBody("Renamed")))
                .andExpect(status().isOk());
        mockMvc.perform(withToken(get("/admin/training/puzzle-detail/" + ownPuzzleId), owner))
                .andExpect(status().isOk());
        mockMvc.perform(withToken(get("/admin/pack-detail").param("packId", String.valueOf(ownPackId)), owner)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    @Test
    void packAdmin_WhenTouchingAnotherAdminsPack_ThenDenied() throws Exception {
        List<MockHttpServletRequestBuilder> requests = List.of(
                patch("/api/training/pack/" + ownPackId).content(packBody("Taken")),
                delete("/api/training/pack/" + ownPackId),
                post("/api/training/pack/translation").content(
                        "{\"packId\":" + ownPackId + ",\"langCode\":\"KO\",\"title\":\"t\",\"author\":\"a\"}"),
                post("/api/training/puzzle").content("{\"packId\":" + ownPackId
                        + ",\"puzzleIndex\":1,\"boardStatus\":\"h8h9\",\"answer\":\"i9\",\"depth\":1,\"winColor\":\"BLACK\"}"),
                patch("/api/training/puzzle/" + ownPuzzleId).content(modifyBody(ownPackId)),
                delete("/api/training/puzzle/" + ownPuzzleId),
                get("/admin/training/pack/" + ownPackId),
                get("/admin/training/puzzle/" + ownPackId),
                get("/admin/training/puzzle-detail/" + ownPuzzleId));
        for (MockHttpServletRequestBuilder request : requests) {
            mockMvc.perform(withToken(request, otherPackAdmin))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorResponse.code").value("A4031"));
        }

        assertThat(packRepository.findById(ownPackId)).isPresent();
        assertThat(trainingPuzzleRepository.findById(ownPuzzleId)).isPresent();
    }

    @Test
    void packAdmin_WhenOpeningAnotherAdminsPackPage_ThenRedirectedToForbidden() throws Exception {
        String packId = String.valueOf(ownPackId);
        List<MockHttpServletRequestBuilder> pages = List.of(
                get("/admin/pack-detail").param("packId", packId),
                get("/admin/pack-create").param("packId", packId),
                get("/admin/puzzle-add").param("packId", packId),
                get("/admin/puzzle-edit").param("packId", packId).param("puzzleId", String.valueOf(ownPuzzleId)));
        for (MockHttpServletRequestBuilder page : pages) {
            mockMvc.perform(withToken(page, otherPackAdmin).accept(MediaType.TEXT_HTML))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/forbidden"));
        }
    }

    @Test
    void packAdmin_WhenMovingOwnPuzzleIntoPackWithoutCreator_ThenDenied() throws Exception {
        mockMvc.perform(withToken(patch("/api/training/puzzle/" + ownPuzzleId), owner)
                        .content(modifyBody(legacyPackId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorResponse.code").value("A4031"));

        assertThat(trainingPuzzleRepository.findById(ownPuzzleId).orElseThrow().getPack().getId()).isEqualTo(ownPackId);
    }

    @Test
    void packAdmin_WhenListingPacks_ThenSeesOnlyOwnEvenWithoutManageableFlag() throws Exception {
        Long otherPackId = createPack(otherPackAdmin);

        assertThat(listPackIds(otherPackAdmin, false)).contains(otherPackId).doesNotContain(ownPackId, legacyPackId);
        assertThat(listPackIds(otherPackAdmin, true)).contains(otherPackId).doesNotContain(ownPackId, legacyPackId);
    }

    @Test
    void operator_WhenManagingAnyPack_ThenAllowedAndSeesCreator() throws Exception {
        UserEntity operator = createAdmin(PACK_MANAGE, OPERATION_MANAGE);

        mockMvc.perform(withToken(patch("/api/training/pack/" + ownPackId), operator).content(packBody("Reviewed")))
                .andExpect(status().isOk());
        mockMvc.perform(withToken(patch("/api/training/pack/" + legacyPackId), operator).content(packBody("Legacy")))
                .andExpect(status().isOk());

        String body = list(operator, true);
        assertThat(ids(body)).contains(ownPackId, legacyPackId);
        List<String> ownerEmails = JsonPath.read(body, "$.response[?(@.id == " + ownPackId + ")].creatorEmail");
        assertThat(ownerEmails).containsExactly(owner.getEmail());
    }

    @Test
    void aiResponseAdmin_WhenReadingPacks_ThenSeesEveryPackButPackPagesListOnlyOwn() throws Exception {
        UserEntity aiAdmin = createAdmin(AI_RESPONSE_MANAGE);
        UserEntity packAndAiAdmin = createAdmin(PACK_MANAGE, AI_RESPONSE_MANAGE);

        mockMvc.perform(withToken(get("/admin/training/pack/" + ownPackId), aiAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(withToken(get("/admin/training/puzzle-detail/" + ownPuzzleId), aiAdmin))
                .andExpect(status().isOk());
        assertThat(listPackIds(aiAdmin, false)).contains(ownPackId, legacyPackId);

        Long mixedPackId = createPack(packAndAiAdmin);
        assertThat(listPackIds(packAndAiAdmin, false)).contains(ownPackId, mixedPackId);
        assertThat(listPackIds(packAndAiAdmin, true)).contains(mixedPackId).doesNotContain(ownPackId);
        mockMvc.perform(withToken(patch("/api/training/pack/" + ownPackId), packAndAiAdmin).content(packBody("Taken")))
                .andExpect(status().isForbidden());
    }

    private Long createPack(UserEntity admin) throws Exception {
        String body = mockMvc.perform(withToken(post("/api/training/pack"), admin).content(packBody("Draft")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(body, "$.response").longValue();
    }

    private List<Long> listPackIds(UserEntity admin, boolean manageable) throws Exception {
        return ids(list(admin, manageable));
    }

    private String list(UserEntity admin, boolean manageable) throws Exception {
        return mockMvc.perform(withToken(get("/admin/training/pack")
                        .param("difficulty", "LOW")
                        .param("manageable", String.valueOf(manageable)), admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static List<Long> ids(String body) {
        List<Number> ids = JsonPath.read(body, "$.response[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    private static String modifyBody(Long packId) {
        return "{\"packId\":" + packId + ",\"boardStatus\":\"" + BOARD + "\",\"answer\":\"c6\",\"depth\":1,\"winColor\":\"BLACK\"}";
    }

    private static String packBody(String title) {
        return "{\"info\":[{\"langCode\":\"EN\",\"title\":\"" + title + "\",\"author\":\"Kang\",\"description\":\"\"}],"
                + "\"price\":0,\"difficulty\":\"LOW\"}";
    }

    private UserEntity createAdmin(AdminPermission first, AdminPermission... rest) {
        UserEntity user = userRepository.save(
                TestUserFactory.createTestUser("own-" + UUID.randomUUID().toString().substring(0, 8), 1000));
        adminRepository.save(Admin.builder().user(user).permissions(EnumSet.of(first, rest)).build());
        return user;
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, UserEntity user) {
        return request
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(user.getId(), "test-session"))
                .header(AppKeyAuthenticationFilter.APP_KEY_HEADER, "test-app-key");
    }
}
