package com.renzzle.backend.domain.auth.service;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final String CLAIM_USER_ID_KEY = "userId";
    private final String CLAIM_EMAIL_KEY = "email";
    private final SecretKey secretKey;

    private String createToken(Map<String, Object> claims, int validMin) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + (validMin * 60000L));

        return Jwts.builder()
                .setIssuedAt(now)
                .setExpiration(validity)
                .setClaims(claims)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private Jws<Claims> parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
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
        return createToken(Map.of(CLAIM_USER_ID_KEY, userId), 60);
    }

    public String createRefreshToken(long userId) {
        return createToken(Map.of(CLAIM_USER_ID_KEY, userId), 60 * 24 * 14);
    }

    public String createAuthVerityToken(String email) {
        return createToken(Map.of(CLAIM_EMAIL_KEY, email), 5);
    }

    public long getUserId(String token) {
        Jws<Claims> claims = parseToken(token);
        Object userId = claims.getBody().get(CLAIM_USER_ID_KEY);

        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        } else {
            throw new CustomException(ErrorCode.ILLEGAL_TOKEN);
        }
    }

    public String getEmail(String token) {
        Jws<Claims> claims = parseToken(token);
        Object email = claims.getBody().get(CLAIM_EMAIL_KEY);

        if(email instanceof String) {
            return (String) email;
        } else {
            throw new CustomException(ErrorCode.ILLEGAL_TOKEN);
        }
    }

}
