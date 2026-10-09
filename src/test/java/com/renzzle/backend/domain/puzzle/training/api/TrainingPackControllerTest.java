package com.renzzle.backend.domain.puzzle.training.api;

import com.jayway.jsonpath.JsonPath;
import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.domain.Admin;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        admin = adminRepository.save(Admin.builder().user(user).build());
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
                        .content(body(translation("KO", "Beginner KO", "Kang"), translation("EN", "Beginner", "Kang"))))
                .andExpect(status().isOk())
                .andReturn();

        createdPackId = JsonPath.<Number>read(result.getResponse().getContentAsString(), "$.response").longValue();
        assertThat(packTranslationRepository.findAllByPack_Id(createdPackId)).hasSize(2);
    }

    @Test
    void createPack_WhenSameLanguageTwice_ThenRejectsAndSavesNothing() throws Exception {
        // A second row for one language breaks the pack list for everyone reading in that language
        assertRejectedWithoutSaving(body(translation("KO", "Beginner", "Kang"), translation("ko", "Beginner 2", "Kang")));
    }

    @Test
    void createPack_WhenTranslationTitleBlank_ThenRejectsAndSavesNothing() throws Exception {
        assertRejectedWithoutSaving(body(translation("KO", " ", "Kang")));
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

    private static String translation(String langCode, String title, String author) {
        return "{\"langCode\":\"" + langCode + "\",\"title\":\"" + title + "\",\"author\":\"" + author
                + "\",\"description\":\"\"}";
    }

    private static String body(String... translations) {
        return "{\"info\":[" + String.join(",", translations) + "],\"price\":0,\"difficulty\":\"LOW\"}";
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(user.getId(), "test-session"))
                .header(AppKeyAuthenticationFilter.APP_KEY_HEADER, "test-app-key");
    }

}
