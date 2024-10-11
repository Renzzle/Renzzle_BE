package com.renzzle.backend.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ErrorCode errorCode = ErrorCode.ADMIN_ACCESS_DENIED;
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            ErrorResponse errorResponse = ErrorResponse.of(errorCode);
            ApiResponse<Object> objectApiResponse = ApiResponse.create(false, null, errorResponse);
            String responseBody = new ObjectMapper().writeValueAsString(objectApiResponse);
            response.getWriter().write(responseBody);
        } catch(Exception e) {
            log.error(e.getMessage());
        }
    }

}
