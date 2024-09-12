package com.renzzle.backend.global.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${JWT_SECRET_KEY}")
    private String JWT_SECRET_KEY;

    // for jwt provider
    @Bean
    public SecretKey secretKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .formLogin(FormLoginConfigurer::disable)
                .sessionManagement((sessionManagement) ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );
        return httpSecurity.build();
    }

}
