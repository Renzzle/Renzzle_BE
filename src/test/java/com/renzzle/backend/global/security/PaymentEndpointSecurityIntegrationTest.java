package com.renzzle.backend.global.security;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.auth.api.response.LoginResponse;
import com.renzzle.backend.domain.auth.service.AuthService;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.ErrorCode;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
class PaymentEndpointSecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;

    private UserEntity user;
    private LoginResponse tokens;

    @BeforeEach
    void setUp() {
        user = userRepository.save(
                TestUserFactory.createTestUser("pay-" + UUID.randomUUID().toString().substring(0, 8), 1000));
        tokens = authService.createAuthTokens(user.getId());
    }

    @AfterEach
    void tearDown() {
        authService.revokeAllSessions(user.getId());
        userRepository.delete(user);
    }

    @Test
    void appleNotification_WithoutAppKeyOrToken_ThenReachesTheHandler() throws Exception {
        // The handler rejects the malformed payload, which shows security let Apple's request through
        mockMvc.perform(post("/webhooks/apple/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"signedPayload\":\"not-a-jws\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorResponse.code").value(ErrorCode.INVALID_PAYMENT_REQUEST.getCode()));
    }

    @Test
    void adminPaymentApi_WhenRegularUser_ThenForbidden() throws Exception {
        mockMvc.perform(get("/admin/payment/purchases")
                        .param("keyword", "anything")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorResponse.code").value(ErrorCode.ADMIN_ACCESS_DENIED.getCode()));
    }

    @Test
    void adminAppleTestNotification_WhenRegularUser_ThenForbidden() throws Exception {
        mockMvc.perform(post("/admin/payment/apple/test-notification")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden());
    }
}
