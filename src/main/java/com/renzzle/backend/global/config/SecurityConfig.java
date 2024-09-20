package com.renzzle.backend.global.config;

import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.user.dao.UserRepository;
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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        List<RequestMatcher> permitAllRequestMatchers = Arrays.asList(
                AntPathRequestMatcher.antMatcher("/"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/test/**"),
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/test/**"),
                AntPathRequestMatcher.antMatcher(HttpMethod.DELETE, "/api/test/**"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/email"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/confirmCode"),
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/auth/duplicate/**"),
                AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/signup"),
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/swagger-ui/**"),
                AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/v3/api-docs/**")
        );

        return httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .formLogin(FormLoginConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement((sessionManagement) ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(request -> request
                        .requestMatchers(permitAllRequestMatchers.toArray(new RequestMatcher[0])).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, userRepository, permitAllRequestMatchers), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

}
