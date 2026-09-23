package com.renzzle.backend.global.security;

import com.renzzle.backend.global.exception.ErrorCode;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import static com.renzzle.backend.domain.auth.domain.Admin.ADMIN_PREFIX;

@Slf4j
@RequiredArgsConstructor
public class AppKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String APP_KEY_HEADER = "x-app-key";

    private final Set<String> appKeys;
    private final RequestMatcher protectedRequestMatcher;

    @Override
    protected boolean shouldNotFilter(@Nonnull HttpServletRequest request) {
        return !protectedRequestMatcher.matches(request);
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain)
            throws ServletException, IOException {
        if (isAdminRequest() || isValidAppKey(request.getHeader(APP_KEY_HEADER))) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Blocked request with missing or invalid app key: {} {}", request.getMethod(), request.getRequestURI());
        SecurityErrorResponder.writeJsonError(response, ErrorCode.INVALID_APP_KEY);
    }

    private boolean isAdminRequest() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ADMIN_PREFIX::equals);
    }

    private boolean isValidAppKey(String requestAppKey) {
        if (requestAppKey == null) {
            return false;
        }

        byte[] requestKey = requestAppKey.getBytes(StandardCharsets.UTF_8);
        boolean matched = false;
        for (String appKey : appKeys) {
            // Not short circuited, so the time spent here does not depend on which key matches
            matched |= MessageDigest.isEqual(requestKey, appKey.getBytes(StandardCharsets.UTF_8));
        }
        return matched;
    }

}
