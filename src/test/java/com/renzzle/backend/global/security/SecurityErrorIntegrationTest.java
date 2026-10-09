package com.renzzle.backend.global.security;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.auth.api.response.LoginResponse;
import com.renzzle.backend.domain.auth.service.AuthService;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.ErrorCode;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
class SecurityErrorIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;

    private UserEntity user;
    private LoginResponse tokens;

    @BeforeEach
    void setUp() {
        user = userRepository.save(
                TestUserFactory.createTestUser("error-" + UUID.randomUUID().toString().substring(0, 8), 1000));
        tokens = authService.createAuthTokens(user.getId());
    }

    @AfterEach
    void tearDown() {
        authService.revokeAllSessions(user.getId());
        userRepository.delete(user);
    }

    @Test
    void request_WhenPathDoesNotExist_ThenReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/no-such-api")
                        .header(AppKeyAuthenticationFilter.APP_KEY_HEADER, "test-app-key")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorResponse.code").value(ErrorCode.GLOBAL_NOT_FOUND.getCode()));
    }

    @Test
    void errorDispatch_WhenOriginalRequestFailed_ThenKeepsItsStatus() throws Exception {
        // The container forwards a failed request to /error without the login it was made with
        mockMvc.perform(get("/error").with(request -> {
                    request.setDispatcherType(DispatcherType.ERROR);
                    request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/user");
                    request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
                    return request;
                }))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminLogout_WhenAdminTokenIsInvalid_ThenRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/logout").cookie(new Cookie("admin_accessToken", "expired.admin.token")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

}
