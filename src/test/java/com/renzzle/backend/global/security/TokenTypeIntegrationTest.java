package com.renzzle.backend.global.security;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.auth.api.response.LoginResponse;
import com.renzzle.backend.domain.auth.service.AuthService;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
class TokenTypeIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;

    private UserEntity user;
    private LoginResponse tokens;

    @BeforeEach
    void setUp() {
        user = userRepository.save(
                TestUserFactory.createTestUser("token-" + UUID.randomUUID().toString().substring(0, 8), 1000));
        tokens = authService.createAuthTokens(user.getId());
    }

    @AfterEach
    void tearDown() {
        authService.revokeAllSessions(user.getId());
        userRepository.delete(user);
    }

    @Test
    void request_WhenRefreshTokenUsedAsAccessToken_ThenRejectsIt() throws Exception {
        mockMvc.perform(withApp(get("/api/app-info"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.refreshToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorResponse.code").value("J4010"));
    }

    @Test
    void request_WhenAccessTokenUsed_ThenAccepts() throws Exception {
        mockMvc.perform(withApp(get("/api/app-info"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void reissue_WhenRefreshTokenAlreadyUsed_ThenOnlyTheFirstReissueSucceeds() throws Exception {
        mockMvc.perform(reissue(tokens.refreshToken()))
                .andExpect(status().isOk());

        mockMvc.perform(reissue(tokens.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reissue_WhenAccessTokenSent_ThenRejectsIt() throws Exception {
        mockMvc.perform(reissue(tokens.accessToken()))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder reissue(String refreshToken) {
        return withApp(post("/api/auth/reissueToken"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}");
    }

    private MockHttpServletRequestBuilder withApp(MockHttpServletRequestBuilder request) {
        return request.header(AppKeyAuthenticationFilter.APP_KEY_HEADER, "test-app-key");
    }

}
