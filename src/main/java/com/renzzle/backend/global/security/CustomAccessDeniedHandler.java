package com.renzzle.backend.global.security;

import com.renzzle.backend.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

import static com.renzzle.backend.domain.auth.domain.Admin.ADMIN_PREFIX;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final String FORBIDDEN_PAGE = "/admin/forbidden";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        // An admin missing one menu's permission stays signed in instead of going back to login
        if (isAdmin()) {
            if (SecurityErrorResponder.isBrowserRequest(request)) {
                response.sendRedirect(FORBIDDEN_PAGE);
            } else {
                SecurityErrorResponder.writeJsonError(response, ErrorCode.ADMIN_PERMISSION_DENIED);
            }
            return;
        }
        SecurityErrorResponder.respond(request, response, ErrorCode.ADMIN_ACCESS_DENIED);
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ADMIN_PREFIX::equals);
    }

}
