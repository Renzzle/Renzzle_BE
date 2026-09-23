package com.renzzle.backend.global.config;

import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.global.security.AppKeyAuthenticationFilter;
import com.renzzle.backend.global.security.CustomAccessDeniedHandler;
import com.renzzle.backend.global.security.CustomAuthenticationEntryPoint;
import com.renzzle.backend.global.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.util.StringUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.renzzle.backend.domain.auth.domain.Admin.ADMIN_PREFIX;

@Slf4j
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
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, @Value("${app.key}") String appKey) throws Exception {
        // Every API request must carry an app key, whether or not it needs a token
        RequestMatcher appKeyRequestMatcher = AntPathRequestMatcher.antMatcher("/api/**");
        Set<String> appKeys = parseAppKeys(appKey);

        List<RequestMatcher> permitAllRequestMatchers = Arrays.asList(
                AntPathRequestMatcher.antMatcher("/admin"),  // Admin login page (excluded from JWT filter)
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/admin/login"),  // Admin login API (called without a token)
                AntPathRequestMatcher.antMatcher("/assets/**"),
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/favicon.ico"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/email"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/confirmCode"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/password/reset/email"),
                AntPathRequestMatcher.antMatcher(HttpMethod.PATCH, "/api/auth/password/reset"),
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/auth/duplicate/**"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/login"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/signup"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/reissueToken"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/test-token"),  // only mapped when docs.enabled=true
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/docs"),  // Scalar API reference page
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/v3/api-docs/**"),
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/actuator/**")
        );

        httpSecurity.csrf(AbstractHttpConfigurer::disable)
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
                        .requestMatchers(HttpMethod.GET, "/admin/community-puzzles").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/notices").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/puzzle-cache").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/puzzle-cache/training-pack").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/puzzle-cache/board").hasAuthority(ADMIN_PREFIX)
                        // Admin-only query APIs (for the dashboard)
                        .requestMatchers(HttpMethod.GET, "/admin/training/pack").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/training/pack/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/training/puzzle/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/training/puzzle-detail/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/community/puzzle-detail/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/community/puzzle-manage/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/notice/**").hasAuthority(ADMIN_PREFIX)
                        // Admin-only create/update/delete APIs
                        .requestMatchers(HttpMethod.POST, "/api/training/puzzle").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.POST, "/api/training/pack").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.PATCH, "/api/training/pack/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.DELETE, "/api/training/pack/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.POST, "/api/training/pack/translation").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.PATCH, "/api/training/puzzle/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.PATCH, "/admin/community/puzzle-manage/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.DELETE, "/api/training/puzzle/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.POST, "/admin/notice/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.PATCH, "/admin/notice/**").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.DELETE, "/admin/notice/**").hasAuthority(ADMIN_PREFIX)
                        // All remaining requests require authentication (including regular users)
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .accessDeniedHandler(accessDeniedHandler)
                                .authenticationEntryPoint(authenticationEntryPoint)
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, userRepository, adminRepository, permitAllRequestMatchers), UsernamePasswordAuthenticationFilter.class);

        if (!appKeys.isEmpty()) {
            // Runs after authentication so admin dashboard requests, which cannot hold the key in a browser, are exempt
            httpSecurity.addFilterAfter(new AppKeyAuthenticationFilter(appKeys, appKeyRequestMatcher), JwtAuthenticationFilter.class);
        } else {
            log.warn("APP_KEY is empty, so app key verification is disabled");
        }

        return httpSecurity.build();
    }

    // APP_KEY holds a comma separated list, so a new key can be accepted before the old one is dropped
    private Set<String> parseAppKeys(String appKey) {
        return Arrays.stream(appKey.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }
}
