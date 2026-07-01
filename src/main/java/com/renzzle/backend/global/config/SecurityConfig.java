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

import static com.renzzle.backend.domain.auth.domain.Admin.ADMIN_PREFIX;

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
                AntPathRequestMatcher.antMatcher("/admin"),  // Admin login page (excluded from JWT filter)
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/admin/login"),  // Admin login API (called without a token)
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
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(request -> request
                        .requestMatchers(permitAllRequestMatchers.toArray(new RequestMatcher[0])).permitAll()
                        // Admin logout must always be accessible regardless of token state
                        .requestMatchers(HttpMethod.GET, "/admin/logout").permitAll()
                        // Admin page (when the token is expired the GET request itself fails, so no separate verify is needed)
                        .requestMatchers(HttpMethod.GET, "/admin/dashboard").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/pack-list").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/pack-create").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/pack-detail").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/puzzle-add").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/puzzle-edit").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/puzzle-cache").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/puzzle-cache/board").hasAuthority(ADMIN_PREFIX)
                        // Admin-only query APIs (for the dashboard)
                        .requestMatchers(HttpMethod.GET, "/admin/training/pack").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/training/pack/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/training/puzzle/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/training/puzzle-detail/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/community/puzzle-detail/**").hasAuthority(ADMIN_PREFIX)
                        // Admin-only create/update/delete APIs
                        .requestMatchers(HttpMethod.POST, "/api/training/puzzle").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.POST, "/api/training/pack").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.PATCH, "/api/training/pack/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.POST, "/api/training/pack/translation").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.PATCH, "/api/training/puzzle/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.DELETE, "/api/training/puzzle/**").hasAuthority(ADMIN_PREFIX)
                        // All remaining requests require authentication (including regular users)
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
