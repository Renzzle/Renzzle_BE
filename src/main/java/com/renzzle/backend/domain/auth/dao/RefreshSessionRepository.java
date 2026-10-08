package com.renzzle.backend.domain.auth.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static com.renzzle.backend.domain.auth.service.JwtProvider.REFRESH_TOKEN_VALID_MINUTE;

// Login sessions per device, each holding its latest refresh token
@Repository
@RequiredArgsConstructor
public class RefreshSessionRepository {

    private static final String SESSION_KEY_PREFIX = "refreshSession:";
    private static final String USER_SESSIONS_KEY_PREFIX = "refreshSessions:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(REFRESH_TOKEN_VALID_MINUTE);

    private final StringRedisTemplate redisTemplate;

    // Saving an existing session replaces its token, so the old one stops working
    public void save(long userId, String sessionId, String refreshToken) {
        redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + sessionId, refreshToken, SESSION_TTL);
        String userSessionsKey = USER_SESSIONS_KEY_PREFIX + userId;
        redisTemplate.opsForSet().add(userSessionsKey, sessionId);
        redisTemplate.expire(userSessionsKey, SESSION_TTL);
    }

    public Optional<String> findToken(String sessionId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + sessionId));
    }

    public void delete(long userId, String sessionId) {
        redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
        redisTemplate.opsForSet().remove(USER_SESSIONS_KEY_PREFIX + userId, sessionId);
    }

    public void deleteAll(long userId) {
        deleteAllExcept(userId, null);
    }

    public void deleteAllExcept(long userId, String keptSessionId) {
        Set<String> sessionIds = redisTemplate.opsForSet().members(USER_SESSIONS_KEY_PREFIX + userId);
        if (sessionIds == null) {
            return;
        }
        for (String sessionId : sessionIds) {
            if (!sessionId.equals(keptSessionId)) {
                delete(userId, sessionId);
            }
        }
    }

}
