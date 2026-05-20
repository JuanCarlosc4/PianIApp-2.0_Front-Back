package com.piania.core.config;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.piania.core.security.CustomAccessDeniedHandler;
import com.piania.core.security.CustomAuthenticationEntryPoint;
import com.piania.core.security.InternalAuthenticationFilter;
import com.piania.core.security.AuditFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final InternalAuthenticationFilter internalAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final AuditFilter auditFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/piania/announcements").permitAll()
                .anyRequest().authenticated()
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ✅ Primero autenticamos desde headers internos
            .addFilterBefore(
                    internalAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            )

            // ✅ Luego registramos auditoría
            .addFilterAfter(
                    auditFilter,
                    InternalAuthenticationFilter.class
            );

        return http.build();
    }
}
