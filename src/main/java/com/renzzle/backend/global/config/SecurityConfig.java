package com.renzzle.backend.global.config;

import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.global.security.CustomAccessDeniedHandler;
import com.renzzle.backend.global.security.CustomAuthenticationEntryPoint;
import com.renzzle.backend.global.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import java.util.Arrays;
import java.util.List;

import static com.renzzle.backend.domain.auth.domain.Admin.ADMIN;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        List<RequestMatcher> permitAllRequestMatchers = Arrays.asList(
                AntPathRequestMatcher.antMatcher("/api/test/**"),
                AntPathRequestMatcher.antMatcher("/admin"),  // 어드민 로그인 페이지 (JWT 필터 제외)
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/admin/login"),  // 어드민 로그인 API (토큰 없이 호출)
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/email"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/confirmCode"),
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/auth/duplicate/**"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/login"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/signup"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/reissueToken"),
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/swagger-ui/**"),
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/v3/api-docs/**"),
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/actuator/**")
        );

        return httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .formLogin(FormLoginConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement((sessionManagement) ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(request -> request
                        .requestMatchers(permitAllRequestMatchers.toArray(new RequestMatcher[0])).permitAll()
                        // Admin 로그아웃은 토큰 상태와 무관하게 항상 접근 가능해야 함
                        .requestMatchers(HttpMethod.GET, "/admin/logout").permitAll()
                        // Admin 페이지 (토큰 만료 시 GET 요청 자체가 실패하므로 별도 verify 불필요)
                        .requestMatchers(HttpMethod.GET, "/admin/dashboard").hasAuthority(ADMIN)
                        // Admin 전용 조회 API (대시보드용)
                        .requestMatchers(HttpMethod.GET, "/admin/training/pack").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/admin/training/puzzle/**").hasAuthority(ADMIN)
                        // Admin 전용 생성/수정/삭제 API
                        .requestMatchers(HttpMethod.POST, "/api/training/puzzle").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/training/pack").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/training/pack/translation").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/training/puzzle/**").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/training/puzzle/**").hasAuthority(ADMIN)
                        // 나머지 모든 요청은 인증 필요 (일반 사용자 포함)
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .accessDeniedHandler(accessDeniedHandler)
                                .authenticationEntryPoint(authenticationEntryPoint)
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, userRepository, adminRepository, permitAllRequestMatchers), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
