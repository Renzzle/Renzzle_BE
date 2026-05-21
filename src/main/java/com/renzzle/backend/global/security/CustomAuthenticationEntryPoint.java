package com.renzzle.backend.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * 인증 실패(401) 처리: 토큰 없음, 토큰 만료 등
 * - HTML 요청(브라우저): /admin 로그인 페이지로 redirect
 * - API 요청: JSON 응답
 */
@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // HTML 요청(브라우저)인 경우 로그인 페이지로 redirect
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            response.sendRedirect("/admin");
            return;
        }

        // API 요청인 경우 JSON 응답
        ErrorCode errorCode = ErrorCode.ILLEGAL_TOKEN;
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
