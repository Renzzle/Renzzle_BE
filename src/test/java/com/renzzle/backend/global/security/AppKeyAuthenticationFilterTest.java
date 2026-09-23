package com.renzzle.backend.global.security;

import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import java.util.List;
import java.util.Set;
import static com.renzzle.backend.domain.auth.domain.Admin.ADMIN_PREFIX;
import static com.renzzle.backend.global.security.AppKeyAuthenticationFilter.APP_KEY_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppKeyAuthenticationFilterTest {

    private static final String APP_KEY = "test-app-key";
    private static final String PREVIOUS_APP_KEY = "previous-test-app-key";

    private AppKeyAuthenticationFilter appKeyAuthenticationFilter;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        appKeyAuthenticationFilter = new AppKeyAuthenticationFilter(Set.of(APP_KEY, PREVIOUS_APP_KEY), AntPathRequestMatcher.antMatcher("/api/**"));
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_WhenAppKeyMatches_ThenPassesRequestOn() throws Exception {
        MockHttpServletRequest request = apiRequest();
        request.addHeader(APP_KEY_HEADER, APP_KEY);

        appKeyAuthenticationFilter.doFilter(request, response, filterChain);

        assertSame(request, filterChain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilter_WhenAnyConfiguredAppKeyMatches_ThenPassesRequestOn() throws Exception {
        MockHttpServletRequest request = apiRequest();
        request.addHeader(APP_KEY_HEADER, PREVIOUS_APP_KEY);

        appKeyAuthenticationFilter.doFilter(request, response, filterChain);

        assertSame(request, filterChain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilter_WhenAppKeyIsMissing_ThenRejectsRequest() throws Exception {
        appKeyAuthenticationFilter.doFilter(apiRequest(), response, filterChain);

        assertRejected();
    }

    @Test
    void doFilter_WhenAppKeyDoesNotMatch_ThenRejectsRequest() throws Exception {
        MockHttpServletRequest request = apiRequest();
        request.addHeader(APP_KEY_HEADER, "other-app-key");

        appKeyAuthenticationFilter.doFilter(request, response, filterChain);

        assertRejected();
    }

    @Test
    void doFilter_WhenAuthenticatedUserIsNotAdmin_ThenRejectsRequestWithoutAppKey() throws Exception {
        authenticateWith("USER");

        appKeyAuthenticationFilter.doFilter(apiRequest(), response, filterChain);

        assertRejected();
    }

    @Test
    void doFilter_WhenAuthenticatedUserIsAdmin_ThenPassesRequestOnWithoutAppKey() throws Exception {
        authenticateWith(ADMIN_PREFIX);
        MockHttpServletRequest request = apiRequest();

        appKeyAuthenticationFilter.doFilter(request, response, filterChain);

        assertSame(request, filterChain.getRequest());
    }

    @Test
    void doFilter_WhenRequestIsNotApi_ThenPassesRequestOnWithoutAppKey() throws Exception {
        MockHttpServletRequest request = request("GET", "/admin/dashboard");

        appKeyAuthenticationFilter.doFilter(request, response, filterChain);

        assertSame(request, filterChain.getRequest());
    }

    private MockHttpServletRequest apiRequest() {
        return request("POST", "/api/auth/login");
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);  // request matchers resolve the path from the servlet path
        return request;
    }

    private void authenticateWith(String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("user", null, List.of(new SimpleGrantedAuthority(authority)))
        );
    }

    private void assertRejected() throws Exception {
        assertNull(filterChain.getRequest());
        assertEquals(ErrorCode.INVALID_APP_KEY.getStatus().value(), response.getStatus());
        assertTrue(response.getContentAsString().contains(ErrorCode.INVALID_APP_KEY.getCode()));
    }

}
