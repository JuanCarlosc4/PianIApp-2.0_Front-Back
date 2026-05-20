package com.piania.core.security;

import java.io.IOException;
import java.time.LocalDateTime;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.piania.core.entity.AuditLog;
import com.piania.core.repository.AuditLogRepository;

@Component
@RequiredArgsConstructor
public class AuditFilter extends OncePerRequestFilter {

    private final AuditLogRepository auditLogRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        filterChain.doFilter(request, response);

        String email = request.getHeader("X-User-Email");

        if (email != null) {

            AuditLog log = AuditLog.builder()
                    .userEmail(email)
                    .method(request.getMethod())
                    .endpoint(request.getRequestURI())
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(log);
        }
    }
}
