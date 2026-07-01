package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final Clock clock;

    public static final int ACCESS_TOKEN_VALID_MINUTE = 60; // 1 hour
    public static final int ADMIN_ACCESS_TOKEN_VALID_MINUTE = 60 * 12; // 12 hours
    public static final int REFRESH_TOKEN_VALID_MINUTE = 60 * 24 * 14; // 2 weeks
    public static final int AUTH_VERITY_TOKEN_VALID_MINUTE = 5; // 5 minute

    private static final String CLAIM_USER_ID_KEY = "userId";
    private static final String CLAIM_EMAIL_KEY = "email";

    @Value("${spring.jwt.secret}")
    private String jwtSecretKey;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    private String createToken(Map<String, Object> claims, int validMin) {
        Instant now = clock.instant();
        Date issuedAt = Date.from(now);
        Date validity = Date.from(now.plus(validMin, ChronoUnit.MINUTES));

        return Jwts.builder()
                .issuedAt(issuedAt)
                .expiration(validity)
                .claims().add(claims).and()
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    private Jws<Claims> parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_JWT_TOKEN);
        } catch (MalformedJwtException e) {
            throw new CustomException(ErrorCode.MALFORMED_JWT_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new CustomException(ErrorCode.UNSUPPORTED_JWT_TOKEN);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.ILLEGAL_TOKEN);
        } catch (JwtException e) {
            throw new CustomException(ErrorCode.CANNOT_PARSE_TOKEN);
        }
    }

    public String createAccessToken(long userId) {
        return createToken(Map.of(CLAIM_USER_ID_KEY, userId), ACCESS_TOKEN_VALID_MINUTE);
    }

    public String createAdminAccessToken(long userId) {
        return createToken(Map.of(CLAIM_USER_ID_KEY, userId), ADMIN_ACCESS_TOKEN_VALID_MINUTE);
    }

    public String createRefreshToken(long userId) {
        return createToken(Map.of(CLAIM_USER_ID_KEY, userId), REFRESH_TOKEN_VALID_MINUTE);
    }

    public String createAuthVerityToken(String email) {
        return createToken(Map.of(CLAIM_EMAIL_KEY, email), AUTH_VERITY_TOKEN_VALID_MINUTE);
    }

    public long getUserId(String token) {
        Jws<Claims> claims = parseToken(token);
        Object userId = claims.getPayload().get(CLAIM_USER_ID_KEY);

        if (userId instanceof Integer i) {
            return i.longValue();
        } else if (userId instanceof Long l) {
            return l;
        } else {
            throw new CustomException(ErrorCode.ILLEGAL_TOKEN);
        }
    }

    public String getEmail(String token) {
        Jws<Claims> claims = parseToken(token);
        Object email = claims.getPayload().get(CLAIM_EMAIL_KEY);

        if (email instanceof String s) {
            return s;
        } else {
            throw new CustomException(ErrorCode.ILLEGAL_TOKEN);
        }
    }

}
