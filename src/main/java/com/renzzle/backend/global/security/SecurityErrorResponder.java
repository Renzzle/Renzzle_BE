package com.renzzle.backend.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public final class SecurityErrorResponder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String LOGIN_PAGE = "/admin";

    private SecurityErrorResponder() {}

    // Browser (HTML) requests are redirected to the login page; API requests receive a JSON error body.
    public static void respond(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) throws IOException {
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            response.sendRedirect(LOGIN_PAGE);
            return;
        }
        writeJsonError(response, errorCode);
    }

    // Writes the standard ApiResponse error body as JSON.
    public static void writeJsonError(HttpServletResponse response, ErrorCode errorCode) {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            ErrorResponse errorResponse = ErrorResponse.of(errorCode);
            ApiResponse<Object> body = ApiResponse.create(false, null, errorResponse);
            response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
        } catch (Exception e) {
            log.warn("Failed to write error response: {}", e.getMessage());
        }
    }
}
