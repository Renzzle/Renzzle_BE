package com.renzzle.backend.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SecurityErrorResponder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SecurityErrorResponder() {}

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
