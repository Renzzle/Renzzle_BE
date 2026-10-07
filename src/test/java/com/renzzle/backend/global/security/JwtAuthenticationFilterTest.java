package com.renzzle.backend.global.security;

import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
            jwtProvider, mock(UserRepository.class), mock(AdminRepository.class), List.of());
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain filterChain = new MockFilterChain();

    @Test
    void doFilter_WhenAuthorizationHeaderHasNoToken_ThenRejectsAsMissingToken() throws Exception {
        // What the API docs send with an empty token field, after the server trims the trailing space
        jwtAuthenticationFilter.doFilter(requestWithAuthorization("Bearer"), response, filterChain);

        assertRejectedWith(ErrorCode.ILLEGAL_TOKEN);
        verifyNoInteractions(jwtProvider);
    }

    @Test
    void doFilter_WhenBearerTokenIsGiven_ThenParsesWhatFollowsTheScheme() throws Exception {
        when(jwtProvider.getUserId("abc.def.ghi")).thenThrow(new CustomException(ErrorCode.MALFORMED_JWT_TOKEN));

        jwtAuthenticationFilter.doFilter(requestWithAuthorization("Bearer abc.def.ghi"), response, filterChain);

        verify(jwtProvider).getUserId("abc.def.ghi");
        assertRejectedWith(ErrorCode.MALFORMED_JWT_TOKEN);
    }

    private MockHttpServletRequest requestWithAuthorization(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user");
        request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        return request;
    }

    private void assertRejectedWith(ErrorCode errorCode) throws Exception {
        assertNull(filterChain.getRequest());
        assertEquals(errorCode.getStatus().value(), response.getStatus());
        assertTrue(response.getContentAsString().contains(errorCode.getCode()));
    }

}
