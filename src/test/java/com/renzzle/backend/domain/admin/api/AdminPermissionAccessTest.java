package com.renzzle.backend.domain.admin.api;

import com.jayway.jsonpath.JsonPath;
import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.domain.Admin;
import com.renzzle.backend.domain.auth.domain.AdminPermission;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static com.renzzle.backend.domain.auth.domain.AdminPermission.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
class AdminPermissionAccessTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminRepository adminRepository;

    private final List<UserEntity> createdUsers = new ArrayList<>();

    // Deleting the users also drops their admin rows through the database cascade
    @AfterEach
    void tearDown() {
        userRepository.deleteAll(createdUsers);
        createdUsers.clear();
    }

    @Test
    void packAdmin_WhenOpeningAnotherTab_ThenGetsPermissionErrorInsteadOfLogout() throws Exception {
        UserEntity user = createAdmin(PACK_MANAGE);

        mockMvc.perform(withToken(get("/admin/notice/system"), user))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorResponse.code").value("A4031"));
        mockMvc.perform(withToken(get("/admin/notices"), user).accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/forbidden"));
        mockMvc.perform(withToken(get("/admin/pack-list"), user).accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    @Test
    void aiResponseAdmin_WhenReadingPacks_ThenAllowedButCannotChangeThem() throws Exception {
        UserEntity user = createAdmin(AI_RESPONSE_MANAGE);

        mockMvc.perform(withToken(get("/admin/training/pack/999999"), user))
                .andExpect(status().isNotFound());
        mockMvc.perform(withToken(post("/api/training/pack"), user).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorResponse.code").value("A4031"));
    }

    @Test
    void dashboard_WhenAdminOpensIt_ThenGoesToTheirFirstPermittedTab() throws Exception {
        mockMvc.perform(withToken(get("/admin/dashboard"), createAdmin(PACK_MANAGE, OPERATION_MANAGE)))
                .andExpect(redirectedUrl("/admin/pack-list"));
        mockMvc.perform(withToken(get("/admin/dashboard"), createAdmin(AI_RESPONSE_MANAGE)))
                .andExpect(redirectedUrl("/admin/puzzle-cache"));
        mockMvc.perform(withToken(get("/admin/dashboard"), createAdmin(OPERATION_MANAGE)))
                .andExpect(redirectedUrl("/admin/notices"));
    }

    @Test
    void regularUser_WhenCallingAnyAdminPath_ThenDenied() throws Exception {
        UserEntity user = createUser();

        mockMvc.perform(withToken(get("/admin/admin-account"), user))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorResponse.code").value("A403"));
        mockMvc.perform(withToken(get("/admin/not-mapped-yet"), user))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorResponse.code").value("A403"));
    }

    @Test
    void login_WhenAdminHasNoPermission_ThenRejected() throws Exception {
        UserEntity user = userRepository.save(TestUserFactory.createTestUser(uniqueName(), 1000).toBuilder()
                .password(new BCryptPasswordEncoder().encode("password1"))
                .build());
        createdUsers.add(user);
        adminRepository.save(Admin.builder().user(user).build());

        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"password1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorResponse.code").value("A4031"));
    }

    @Test
    void adminAccounts_WhenOperationAdminGrantsAndRevokes_ThenAccessFollowsWithoutNewLogin() throws Exception {
        UserEntity operator = createAdmin(OPERATION_MANAGE);
        UserEntity appUser = createUser();

        MvcResult added = mockMvc.perform(withToken(post("/admin/admin-account"), operator)
                        .content("{\"email\":\"" + appUser.getEmail().toUpperCase() + "\",\"permissions\":[\"PACK_MANAGE\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.permissions[0]").value("PACK_MANAGE"))
                .andReturn();
        long adminId = JsonPath.<Number>read(added.getResponse().getContentAsString(), "$.response.adminId").longValue();

        mockMvc.perform(withToken(get("/admin/training/pack/999999"), appUser))
                .andExpect(status().isNotFound());

        mockMvc.perform(withToken(patch("/admin/admin-account/" + adminId), operator)
                        .content("{\"permissions\":[\"COMMUNITY_MANAGE\"]}"))
                .andExpect(status().isOk());
        mockMvc.perform(withToken(get("/admin/training/pack/999999"), appUser))
                .andExpect(jsonPath("$.errorResponse.code").value("A4031"));

        mockMvc.perform(withToken(delete("/admin/admin-account/" + adminId), operator))
                .andExpect(status().isOk());
        mockMvc.perform(withToken(get("/admin/community/puzzle-manage/1"), appUser))
                .andExpect(jsonPath("$.errorResponse.code").value("A403"));
        assertThat(userRepository.findById(appUser.getId())).isPresent();
    }

    @Test
    void adminAccounts_WhenOperationAdminDropsTheirOwnAccess_ThenRejected() throws Exception {
        UserEntity operator = createAdmin(OPERATION_MANAGE, PACK_MANAGE);
        long myAdminId = adminRepository.findByUser(operator).orElseThrow().getId();

        mockMvc.perform(withToken(patch("/admin/admin-account/" + myAdminId), operator)
                        .content("{\"permissions\":[\"PACK_MANAGE\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorResponse.code").value("A4000"));
        mockMvc.perform(withToken(delete("/admin/admin-account/" + myAdminId), operator))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorResponse.code").value("A4000"));

        assertThat(adminRepository.findByUser(operator).orElseThrow().getPermissions())
                .containsExactlyInAnyOrder(OPERATION_MANAGE, PACK_MANAGE);
    }

    @Test
    void adminAccounts_WhenNoPermissionGiven_ThenRejectedWithoutAddingAdmin() throws Exception {
        UserEntity operator = createAdmin(OPERATION_MANAGE);
        UserEntity appUser = createUser();

        mockMvc.perform(withToken(post("/admin/admin-account"), operator)
                        .content("{\"email\":\"" + appUser.getEmail() + "\",\"permissions\":[]}"))
                .andExpect(status().isBadRequest());

        assertThat(adminRepository.existsByUser(appUser)).isFalse();
    }

    private UserEntity createUser() {
        UserEntity user = userRepository.save(TestUserFactory.createTestUser(uniqueName(), 1000));
        createdUsers.add(user);
        return user;
    }

    private UserEntity createAdmin(AdminPermission first, AdminPermission... rest) {
        UserEntity user = createUser();
        adminRepository.save(Admin.builder().user(user).permissions(EnumSet.of(first, rest)).build());
        return user;
    }

    private static String uniqueName() {
        return "perm-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, UserEntity user) {
        return request
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(user.getId(), "test-session"))
                .header(AppKeyAuthenticationFilter.APP_KEY_HEADER, "test-app-key");
    }
}
