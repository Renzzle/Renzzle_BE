package com.renzzle.backend.global.ratelimit;

import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.security.SecurityErrorResponder;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Per-IP rate limiting for the unauthenticated /api/auth endpoints.
 *
 * <p>Cloudflare already caps short bursts at the edge, but its free plan limits the
 * counting window to 10 seconds, which slow and sustained abuse simply waits out.
 * This filter covers the long windows that the edge cannot express.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // after RequestLoggingFilter, before the security chain
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/auth";
    private static final String CLIENT_IP_HEADER = "X-Real-IP";
    private static final String KEY_PREFIX = "rate-limit:";
    private static final long NOT_LIMITED = -1L;

    private record Rule(String name, RequestMatcher matcher, int limit, Duration window) { }

    // Limits are per client IP. They are deliberately generous because mobile carriers
    // put many subscribers behind one address, and blocking a real user costs more than
    // letting a slow attacker through a little longer.
    private static final List<Rule> RULES = List.of(
            // Every call sends a real email, so this is the only path that costs money
            new Rule("email",
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/email"),
                    10, Duration.ofHours(1)),
            new Rule("password-reset-email",
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/password/reset/email"),
                    10, Duration.ofHours(1)),
            // Guessing a six digit verification code
            new Rule("confirm-code",
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/confirmCode"),
                    20, Duration.ofMinutes(10)),
            // Credential stuffing
            new Rule("login",
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/login"),
                    20, Duration.ofMinutes(10)),
            // Nickname enumeration
            new Rule("duplicate",
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/auth/duplicate/**"),
                    60, Duration.ofMinutes(1))
    );

    private final StringRedisTemplate redisTemplate;

    @Override
    protected boolean shouldNotFilter(@Nonnull HttpServletRequest request) {
        return !request.getRequestURI().startsWith(AUTH_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws ServletException, IOException {
        Rule rule = matchRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        long retryAfter = retryAfterSeconds(rule, clientIp);
        if (retryAfter != NOT_LIMITED) {
            log.warn("Rate limit exceeded: rule={} ip={} {} {}",
                    rule.name(), clientIp, request.getMethod(), request.getRequestURI());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
            SecurityErrorResponder.writeJsonError(response, ErrorCode.EXCEED_AUTH_REQUEST_RATE);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Rule matchRule(HttpServletRequest request) {
        for (Rule rule : RULES) {
            if (rule.matcher().matches(request)) {
                return rule;
            }
        }
        return null;
    }

    // Caddy fills X-Real-IP from Cloudflare's CF-Connecting-IP. The origin firewall only
    // admits Cloudflare, so the header cannot be spoofed from outside the edge.
    private String resolveClientIp(HttpServletRequest request) {
        String header = request.getHeader(CLIENT_IP_HEADER);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        return request.getRemoteAddr();
    }

    // Returns seconds until the window resets, or NOT_LIMITED while the request is allowed.
    private long retryAfterSeconds(Rule rule, String clientIp) {
        String key = KEY_PREFIX + rule.name() + ":" + clientIp;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                return NOT_LIMITED;
            }
            if (count == 1L) {
                redisTemplate.expire(key, rule.window());
                return NOT_LIMITED;
            }
            if (count <= rule.limit()) {
                return NOT_LIMITED;
            }

            Long ttl = redisTemplate.getExpire(key);
            if (ttl == null || ttl < 0) {
                // The counter lost its expiry, so restore it rather than block forever
                redisTemplate.expire(key, rule.window());
                return rule.window().toSeconds();
            }
            return ttl;
        } catch (Exception e) {
            // Redis being unreachable must not take authentication down with it
            log.warn("Rate limit check skipped for rule={}: {}", rule.name(), e.getMessage());
            return NOT_LIMITED;
        }
    }

}
