package com.renzzle.backend.global.config;

import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.global.security.AppKeyAuthenticationFilter;
import com.renzzle.backend.global.security.CustomAccessDeniedHandler;
import com.renzzle.backend.global.security.CustomAuthenticationEntryPoint;
import com.renzzle.backend.global.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
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
import static com.renzzle.backend.domain.auth.domain.AdminPermission.AI_RESPONSE_MANAGE;
import static com.renzzle.backend.domain.auth.domain.AdminPermission.COMMUNITY_MANAGE;
import static com.renzzle.backend.domain.auth.domain.AdminPermission.OPERATION_MANAGE;
import static com.renzzle.backend.domain.auth.domain.AdminPermission.PACK_MANAGE;

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
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/admin/logout"),  // Clears the admin cookie, so it must work with an expired token
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
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/actuator/**"),
                // Called by Apple without a token; the handler trusts nothing in it without asking Apple again
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/webhooks/apple/notifications")
        );

        httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .formLogin(FormLoginConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(request -> request
                        // The container forwards failed requests to /error without their login, so keep their status
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(permitAllRequestMatchers.toArray(new RequestMatcher[0])).permitAll()
                        // Admin pages, one permission per top tab
                        .requestMatchers(HttpMethod.GET, "/admin/dashboard", "/admin/forbidden").hasAuthority(ADMIN_PREFIX)
                        .requestMatchers(HttpMethod.GET, "/admin/pack-list", "/admin/pack-create", "/admin/pack-detail",
                                "/admin/puzzle-add", "/admin/puzzle-edit").hasAuthority(PACK_MANAGE.getAuthority())
                        .requestMatchers(HttpMethod.GET, "/admin/community-puzzles").hasAuthority(COMMUNITY_MANAGE.getAuthority())
                        .requestMatchers(HttpMethod.GET, "/admin/puzzle-cache", "/admin/puzzle-cache/training-pack",
                                "/admin/puzzle-cache/board").hasAuthority(AI_RESPONSE_MANAGE.getAuthority())
                        .requestMatchers(HttpMethod.GET, "/admin/notices", "/admin/payments", "/admin/admin-accounts").hasAuthority(OPERATION_MANAGE.getAuthority())
                        // Read APIs the AI response pages share with the pack and community tabs
                        .requestMatchers(HttpMethod.GET, "/admin/training/**")
                                .hasAnyAuthority(PACK_MANAGE.getAuthority(), AI_RESPONSE_MANAGE.getAuthority())
                        .requestMatchers(HttpMethod.GET, "/admin/community/puzzle-detail/**")
                                .hasAnyAuthority(COMMUNITY_MANAGE.getAuthority(), AI_RESPONSE_MANAGE.getAuthority())
                        // Training pack and puzzle changes
                        .requestMatchers(HttpMethod.POST, "/api/training/pack", "/api/training/pack/translation",
                                "/api/training/puzzle").hasAuthority(PACK_MANAGE.getAuthority())
                        .requestMatchers(HttpMethod.PATCH, "/api/training/pack/**", "/api/training/puzzle/**").hasAuthority(PACK_MANAGE.getAuthority())
                        .requestMatchers(HttpMethod.DELETE, "/api/training/pack/**", "/api/training/puzzle/**").hasAuthority(PACK_MANAGE.getAuthority())
                        .requestMatchers("/admin/community/puzzle-manage/**").hasAuthority(COMMUNITY_MANAGE.getAuthority())
                        // The app saves AI responses itself, but only admins may delete them
                        .requestMatchers(HttpMethod.DELETE, "/api/puzzle/cache/**").hasAuthority(AI_RESPONSE_MANAGE.getAuthority())
                        // Operation: notices, payments, admin accounts, and settings that have no page
                        .requestMatchers("/admin/notice/**", "/admin/payment/**", "/admin/admin-account/**").hasAuthority(OPERATION_MANAGE.getAuthority())
                        .requestMatchers(HttpMethod.POST, "/admin/puzzle/rating/recalculate", "/admin/puzzle/board-key/recalculate",
                                "/admin/puzzle/answer-key/recalculate").hasAuthority(OPERATION_MANAGE.getAuthority())
                        .requestMatchers(HttpMethod.POST, "/api/app-info").hasAuthority(OPERATION_MANAGE.getAuthority())
                        // Any other admin path still needs an admin
                        .requestMatchers("/admin/**").hasAuthority(ADMIN_PREFIX)
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
            // After JWT auth so browser admin requests, which have no app key, pass
            httpSecurity.addFilterAfter(new AppKeyAuthenticationFilter(appKeys, appKeyRequestMatcher), JwtAuthenticationFilter.class);
        } else {
            log.warn("APP_KEY is empty, so app key verification is disabled");
        }

        return httpSecurity.build();
    }

    // Comma separated, so keys can rotate without downtime
    private Set<String> parseAppKeys(String appKey) {
        return Arrays.stream(appKey.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }
}
