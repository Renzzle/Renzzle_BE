package com.renzzle.backend.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.domain.GrantType;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.exception.ErrorResponse;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static com.renzzle.backend.domain.auth.domain.Admin.ADMIN;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final List<RequestMatcher> permitAllRequestMatchers;

    @Override
    protected boolean shouldNotFilter(@Nonnull HttpServletRequest request) {
        return permitAllRequestMatchers.stream().anyMatch(matcher -> matcher.matches(request));
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain) throws ServletException, IOException {
        try {
            String accessToken = resolveToken(request);
            Long userId = jwtProvider.getUserId(accessToken);
            UserDetails userDetails = loadUserByUserId(userId);
            Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(userDetails, userDetails.getPassword(), userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch(CustomException e) {
            handleException(response, e.getErrorCode());
        } catch(Exception e) {
            handleException(response, ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public UserDetails loadUserByUserId(Long userId) throws CustomException {
        Optional<UserEntity> user = userRepository.findById(userId);
        if(user.isEmpty())
            throw new CustomException(ErrorCode.GLOBAL_NOT_FOUND);

        List<String> roles = new ArrayList<>();
        if(adminRepository.existsByUser(user.get()))
            roles.add(ADMIN);

        return new UserDetailsImpl(user.get(), user.get().getPassword(), roles);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if(!StringUtils.hasText(bearerToken)) {
            throw new CustomException(ErrorCode.ILLEGAL_TOKEN);
        }
        if(!bearerToken.startsWith(GrantType.BEARER.getType())) {
            throw new CustomException(ErrorCode.NOT_BEARER_GRANT_TYPE);
        }
        return bearerToken.substring(7);
    }

    private void handleException(HttpServletResponse response, ErrorCode errorCode) {
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
