package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import static com.renzzle.backend.domain.auth.service.JwtTest.JWT_TEST_PROPERTY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = JwtProvider.class)
@TestPropertySource(properties = JWT_TEST_PROPERTY)
public class JwtTest {

    public static final String JWT_TEST_PROPERTY = "spring.jwt.secret=ad3sf2sf98a7sd9f87a0ds98f70a98sd7f098asd70f98";

    @MockBean
    private Clock clock;

    @InjectMocks
    @Autowired
    private JwtProvider jwtProvider;

    private long testUserId;
    private String testEmail;
    private String accessToken;
    private String refreshToken;
    private String authVerityToken;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        testUserId = 1L;
        testEmail = "test@example.com";
        accessToken = jwtProvider.createAccessToken(testUserId);
        refreshToken = jwtProvider.createRefreshToken(testUserId);
        authVerityToken = jwtProvider.createAuthVerityToken(testEmail);

        when(clock.instant()).thenReturn(Instant.now().plusSeconds(1));
    }

    @Test
    public void createAccessToken_ShouldReturnValidToken() {
        assertNotNull(accessToken);
        assertNotEquals(accessToken, jwtProvider.createAccessToken(testUserId));
    }

    @Test
    public void createRefreshToken_ShouldReturnValidToken() {
        assertNotNull(refreshToken);
        assertNotEquals(refreshToken, jwtProvider.createRefreshToken(testUserId));
    }

    @Test
    public void createAuthVerityToken_ShouldReturnValidToken() {
        assertNotNull(authVerityToken);
        assertNotEquals(authVerityToken, jwtProvider.createAuthVerityToken(testEmail));
    }

    @Test
    public void getUserId_ShouldReturnCorrectUserId() {
        long extractedUserId = jwtProvider.getUserId(accessToken);
        assertEquals(testUserId, extractedUserId);
    }

    @Test
    public void getEmail_ShouldReturnCorrectEmail() {
        String extractedEmail = jwtProvider.getEmail(authVerityToken);
        assertEquals(testEmail, extractedEmail);
    }

    @Test
    public void parseToken_WithMalformedToken_ShouldThrowException() {
        String malformedToken = "this.is.not.a.valid.jwt";

        CustomException exception = assertThrows(CustomException.class, () -> {
            jwtProvider.getUserId(malformedToken);
        });

        assertEquals(ErrorCode.MALFORMED_JWT_TOKEN, exception.getErrorCode());
    }

    @Test
    public void parseToken_WithEmptyToken_ShouldThrowException() {
        CustomException exception = assertThrows(CustomException.class, () -> {
            jwtProvider.getUserId("");
        });

        assertEquals(ErrorCode.ILLEGAL_TOKEN, exception.getErrorCode());
    }

}
