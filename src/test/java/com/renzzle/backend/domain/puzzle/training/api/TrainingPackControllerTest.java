package com.renzzle.backend.domain.puzzle.training.api;

import com.jayway.jsonpath.JsonPath;
import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.domain.Admin;
import com.renzzle.backend.domain.auth.domain.AdminPermission;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.puzzle.training.dao.PackRepository;
import com.renzzle.backend.domain.puzzle.training.dao.PackTranslationRepository;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.security.AppKeyAuthenticationFilter;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
class TrainingPackControllerTest {

    private static final String ENDPOINT = "/api/training/pack";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private PackRepository packRepository;
    @Autowired private PackTranslationRepository packTranslationRepository;

    private UserEntity user;
    private Admin admin;
    private Long createdPackId;

    @BeforeEach
    void setUp() {
        user = userRepository.save(
                TestUserFactory.createTestUser("pack-" + UUID.randomUUID().toString().substring(0, 8), 1000));
        admin = adminRepository.save(Admin.builder().user(user).permissions(EnumSet.of(AdminPermission.PACK_MANAGE)).build());
    }

    @AfterEach
    void tearDown() {
        if (createdPackId != null) {
            packTranslationRepository.deleteAll(packTranslationRepository.findAllByPack_Id(createdPackId));
            packRepository.deleteById(createdPackId);
        }
        adminRepository.delete(admin);
        userRepository.delete(user);
    }

    @Test
    void createPack_WhenTranslationsValid_ThenCreatesPack() throws Exception {
        MvcResult result = mockMvc.perform(asAdmin(post(ENDPOINT))
                        .content(body(translation("KO", "Beginner KO"), translation("EN", "Beginner"))))
                .andExpect(status().isOk())
                .andReturn();

        createdPackId = JsonPath.<Number>read(result.getResponse().getContentAsString(), "$.response").longValue();
        assertThat(packTranslationRepository.findAllByPack_Id(createdPackId)).hasSize(2);
    }

    @Test
    void createPack_WhenVisibilityOmitted_ThenStaysPrivateUntilPublished() throws Exception {
        String translation = translation("EN", "Draft");
        MvcResult result = mockMvc.perform(asAdmin(post(ENDPOINT)).content(body(translation)))
                .andExpect(status().isOk())
                .andReturn();
        createdPackId = JsonPath.<Number>read(result.getResponse().getContentAsString(), "$.response").longValue();

        mockMvc.perform(asAdmin(get("/admin/training/pack/" + createdPackId)))
                .andExpect(jsonPath("$.response.isPublic").value(false));

        mockMvc.perform(asAdmin(patch(ENDPOINT + "/" + createdPackId)).content(publicBody(translation)))
                .andExpect(status().isOk());

        mockMvc.perform(asAdmin(get("/admin/training/pack/" + createdPackId)))
                .andExpect(jsonPath("$.response.isPublic").value(true));
    }

    @Test
    void updatePack_WhenPackAdminSetsPriority_ThenRejectsAndKeepsIt() throws Exception {
        String translation = translation("EN", "Mine");
        createdPackId = createPack(translation);

        mockMvc.perform(asAdmin(patch(ENDPOINT + "/" + createdPackId)).content(priorityBody(translation)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorResponse.code").value("A4031"));

        mockMvc.perform(asAdmin(get("/admin/training/pack/" + createdPackId)))
                .andExpect(jsonPath("$.response.priority").value(0));
    }

    @Test
    void updatePack_WhenOperatorSetsPriority_ThenSavesIt() throws Exception {
        String translation = translation("EN", "Featured");
        createdPackId = createPack(translation);
        admin.updatePermissions(EnumSet.of(AdminPermission.PACK_MANAGE, AdminPermission.OPERATION_MANAGE));
        adminRepository.save(admin);

        mockMvc.perform(asAdmin(patch(ENDPOINT + "/" + createdPackId)).content(priorityBody(translation)))
                .andExpect(status().isOk());

        mockMvc.perform(asAdmin(get("/admin/training/pack/" + createdPackId)))
                .andExpect(jsonPath("$.response.priority").value(10));
    }

    @Test
    void createPack_WhenSameLanguageTwice_ThenRejectsAndSavesNothing() throws Exception {
        // A second row for one language breaks the pack list for everyone reading in that language
        assertRejectedWithoutSaving(body(translation("KO", "Beginner"), translation("ko", "Beginner 2")));
    }

    @Test
    void createPack_WhenTranslationTitleBlank_ThenRejectsAndSavesNothing() throws Exception {
        assertRejectedWithoutSaving(body(translation("KO", " ")));
    }

    @Test
    void createPack_WhenNoTranslationGiven_ThenRejectsAndSavesNothing() throws Exception {
        assertRejectedWithoutSaving(body());
    }

    private void assertRejectedWithoutSaving(String body) throws Exception {
        long packCount = packRepository.count();

        mockMvc.perform(asAdmin(post(ENDPOINT)).content(body))
                .andExpect(status().isBadRequest());

        assertThat(packRepository.count()).isEqualTo(packCount);
    }

    private static String translation(String langCode, String title) {
        return "{\"langCode\":\"" + langCode + "\",\"title\":\"" + title + "\",\"author\":\"" + "Kang"
                + "\",\"description\":\"\"}";
    }

    private static String body(String... translations) {
        return "{\"info\":[" + String.join(",", translations) + "],\"price\":0,\"difficulty\":\"LOW\"}";
    }

    private static String publicBody(String... translations) {
        String body = body(translations);
        return body.substring(0, body.length() - 1) + ",\"isPublic\":true}";
    }

    private static String priorityBody(String... translations) {
        String body = body(translations);
        return body.substring(0, body.length() - 1) + ",\"priority\":" + 10 + "}";
    }

    private Long createPack(String translation) throws Exception {
        MvcResult result = mockMvc.perform(asAdmin(post(ENDPOINT)).content(body(translation)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.<Number>read(result.getResponse().getContentAsString(), "$.response").longValue();
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(user.getId(), "test-session"))
                .header(AppKeyAuthenticationFilter.APP_KEY_HEADER, "test-app-key");
    }

}
