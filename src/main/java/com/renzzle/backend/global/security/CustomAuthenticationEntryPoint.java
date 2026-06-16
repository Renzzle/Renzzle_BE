package com.renzzle.backend.global.security;

import com.renzzle.backend.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * Handles authentication failures (401): missing token, expired token, etc.
 * - HTML requests (browser): redirect to the /admin login page
 * - API requests: JSON response
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        SecurityErrorResponder.respond(request, response, ErrorCode.ILLEGAL_TOKEN);
    }
}
