package com.renzzle.backend.domain.appinfo.api;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.appinfo.dao.AppInfoRepository;
import com.renzzle.backend.domain.appinfo.domain.AppInfo;
import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.domain.Admin;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.security.AppKeyAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
class AppInfoControllerTest {

    private static final String ENDPOINT = "/api/app-info";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private AppInfoRepository appInfoRepository;

    private final List<UserEntity> createdUsers = new ArrayList<>();
    private final String tag = "test_tag_" + UUID.randomUUID().toString().substring(0, 8);

    @AfterEach
    void tearDown() {
        appInfoRepository.findByTag(tag).ifPresent(appInfoRepository::delete);
        adminRepository.deleteAll(adminRepository.findAll().stream()
                .filter(admin -> createdUsers.contains(admin.getUser()))
                .toList());
        userRepository.deleteAll(createdUsers);
        createdUsers.clear();
    }

    @Test
    void upsertAppInfo_WhenRegularUser_ThenForbidsAndSavesNothing() throws Exception {
        UserEntity user = createUser();

        mockMvc.perform(withToken(post(ENDPOINT), user).content(body(tag, "https://example.com")))
                .andExpect(status().isForbidden());

        assertThat(appInfoRepository.findByTag(tag)).isEmpty();
    }

    @Test
    void upsertAppInfo_WhenAdminAndNewTag_ThenCreatesTrimmedEntry() throws Exception {
        UserEntity admin = createAdmin();

        mockMvc.perform(withToken(post(ENDPOINT), admin).content(body(" " + tag + " ", "  https://example.com  ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.tag").value(tag))
                .andExpect(jsonPath("$.response.value").value("https://example.com"));

        assertThat(appInfoRepository.findByTag(tag)).get()
                .extracting(AppInfo::getValue).isEqualTo("https://example.com");
    }

    @Test
    void upsertAppInfo_WhenAdminAndTagExists_ThenReplacesValueInSameRow() throws Exception {
        UserEntity admin = createAdmin();
        Long existingId = appInfoRepository.save(AppInfo.builder().tag(tag).value("https://old.com").build()).getId();

        mockMvc.perform(withToken(post(ENDPOINT), admin).content(body(tag, "https://new.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.value").value("https://new.com"));

        AppInfo stored = appInfoRepository.findByTag(tag).orElseThrow();
        assertThat(stored.getId()).isEqualTo(existingId);
        assertThat(stored.getValue()).isEqualTo("https://new.com");
    }

    @Test
    void upsertAppInfo_WhenValueBlank_ThenRejectsAndSavesNothing() throws Exception {
        UserEntity admin = createAdmin();

        mockMvc.perform(withToken(post(ENDPOINT), admin).content(body(tag, " ")))
                .andExpect(status().isBadRequest());

        assertThat(appInfoRepository.findByTag(tag)).isEmpty();
    }

    private UserEntity createUser() {
        UserEntity user = userRepository.save(
                TestUserFactory.createTestUser("info-" + UUID.randomUUID().toString().substring(0, 8), 1000));
        createdUsers.add(user);
        return user;
    }

    private UserEntity createAdmin() {
        UserEntity user = createUser();
        adminRepository.save(Admin.builder().user(user).build());
        return user;
    }

    private static String body(String tag, String value) {
        return "{\"tag\":\"" + tag + "\",\"value\":\"" + value + "\"}";
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, UserEntity user) {
        return request
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(user.getId()))
                .header(AppKeyAuthenticationFilter.APP_KEY_HEADER, "test-app-key");
    }
}
