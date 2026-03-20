package com.lingotlow.backendcore.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        
                        // Tenant creation endpoint - allow without authentication
                        .requestMatchers(HttpMethod.POST, "/api/tenants").permitAll()

                        // API Key endpoints - require API key authentication
                        .requestMatchers("/api/tenants/*/api-keys/**").authenticated()

                        // Tenant management endpoints - require API key authentication
                        .requestMatchers(HttpMethod.GET, "/api/tenants").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tenants/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/tenants/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/tenants/**").authenticated()

                        // Event endpoints - require API key authentication
                        .requestMatchers("/api/ingest/**").authenticated()
                        .requestMatchers("/api/events/**").authenticated()

                        // Any other API endpoint - require authentication
                        .requestMatchers("/api/**").authenticated()

                        // Any other request
                        .anyRequest().authenticated()
                )
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
