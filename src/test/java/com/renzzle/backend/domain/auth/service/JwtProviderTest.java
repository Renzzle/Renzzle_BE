package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.Map;
import static com.renzzle.backend.domain.auth.service.JwtProviderTest.JWT_TEST_PROPERTY;
import static com.renzzle.backend.support.TestTime.FIXED_INSTANT;
import static com.renzzle.backend.support.TestTime.FIXED_ZONE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = JwtProvider.class)
@TestPropertySource(properties = JWT_TEST_PROPERTY)
class JwtProviderTest {

    public static final String JWT_TEST_PROPERTY = "spring.jwt.secret=ad3sf2sf98a7sd9f87a0ds98f70a98sd7f098asd70f98";
    private static final String SESSION_ID = "session-1";

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
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(FIXED_ZONE);

        testUserId = 1L;
        testEmail = "test@example.com";
        accessToken = jwtProvider.createAccessToken(testUserId, SESSION_ID);
        refreshToken = jwtProvider.createRefreshToken(testUserId, SESSION_ID);
        authVerityToken = jwtProvider.createAuthVerityToken(testEmail);

        when(clock.instant()).thenReturn(FIXED_INSTANT.plusSeconds(1));
    }

    @Test
    void createAccessToken_ShouldReturnValidToken() {
        assertNotNull(accessToken);
        assertNotEquals(accessToken, jwtProvider.createAccessToken(testUserId, SESSION_ID));
    }

    @Test
    void createRefreshToken_ShouldReturnValidToken() {
        assertNotNull(refreshToken);
        assertNotEquals(refreshToken, jwtProvider.createRefreshToken(testUserId, SESSION_ID));
    }

    @Test
    void createAuthVerityToken_ShouldReturnValidToken() {
        assertNotNull(authVerityToken);
        assertNotEquals(authVerityToken, jwtProvider.createAuthVerityToken(testEmail));
    }

    @Test
    void parseAccessToken_ShouldReturnUserAndSession() {
        JwtProvider.TokenClaims claims = jwtProvider.parseAccessToken(accessToken);
        assertEquals(testUserId, claims.userId());
        assertEquals(SESSION_ID, claims.sessionId());
    }

    @Test
    void parseRefreshToken_ShouldReturnUserAndSession() {
        JwtProvider.TokenClaims claims = jwtProvider.parseRefreshToken(refreshToken);
        assertEquals(testUserId, claims.userId());
        assertEquals(SESSION_ID, claims.sessionId());
    }

    @Test
    void parseAccessToken_WithAdminToken_ShouldHaveNoSession() {
        JwtProvider.TokenClaims claims = jwtProvider.parseAccessToken(jwtProvider.createAdminAccessToken(testUserId));
        assertEquals(testUserId, claims.userId());
        assertNull(claims.sessionId());
    }

    @Test
    void parseAccessToken_WithRefreshToken_ShouldBeRejected() {
        CustomException exception = assertThrows(CustomException.class,
                () -> jwtProvider.parseAccessToken(refreshToken));
        assertEquals(ErrorCode.EXPIRED_JWT_TOKEN, exception.getErrorCode());
    }

    @Test
    void parseRefreshToken_WithAccessToken_ShouldBeRejected() {
        CustomException exception = assertThrows(CustomException.class,
                () -> jwtProvider.parseRefreshToken(accessToken));
        assertEquals(ErrorCode.EXPIRED_JWT_TOKEN, exception.getErrorCode());
    }

    @Test
    void parseAccessToken_WithTokenFromBeforeTypes_ShouldBeRejected() {
        // Signed with the same key, but issued before tokens carried a type
        String secret = JWT_TEST_PROPERTY.substring(JWT_TEST_PROPERTY.indexOf('=') + 1);
        String legacyToken = Jwts.builder()
                .issuedAt(Date.from(FIXED_INSTANT))
                .expiration(Date.from(FIXED_INSTANT.plusSeconds(3600)))
                .claims().add(Map.of("userId", testUserId)).and()
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        CustomException exception = assertThrows(CustomException.class,
                () -> jwtProvider.parseAccessToken(legacyToken));
        assertEquals(ErrorCode.EXPIRED_JWT_TOKEN, exception.getErrorCode());
    }

    @Test
    void getEmail_ShouldReturnCorrectEmail() {
        String extractedEmail = jwtProvider.getEmail(authVerityToken);
        assertEquals(testEmail, extractedEmail);
    }

    @Test
    void parseToken_WithMalformedToken_ShouldThrowException() {
        String malformedToken = "this.is.not.a.valid.jwt";

        CustomException exception = assertThrows(CustomException.class, () -> {
            jwtProvider.parseAccessToken(malformedToken);
        });

        assertEquals(ErrorCode.MALFORMED_JWT_TOKEN, exception.getErrorCode());
    }

    @Test
    void parseToken_WithEmptyToken_ShouldThrowException() {
        CustomException exception = assertThrows(CustomException.class, () -> {
            jwtProvider.parseAccessToken("");
        });

        assertEquals(ErrorCode.ILLEGAL_TOKEN, exception.getErrorCode());
    }

}
