package com.piania.gateway.security;

import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import io.jsonwebtoken.Claims;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // CORS preflight must be allowed through without auth
        // (Spring WebFlux uses HttpMethod on the request)
        if (exchange.getRequest().getMethod() != null
                && "OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
            return chain.filter(exchange);
        }

        // Public endpoints (no JWT required)
        // Externamente exponemos /piania/** y el gateway reescribe a /piania/api/**,
        // pero para evitar problemas (y para local/dev) permitimos ambos.
        if (path.startsWith("/piania/auth/")
                || path.startsWith("/uploads/")
                || path.startsWith("/actuator/")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")) {
            return chain.filter(exchange);
        }

        if (exchange.getRequest().getMethod() != null
                && "GET".equalsIgnoreCase(exchange.getRequest().getMethod().name())
                && (path.equals("/piania/core/announcements") || path.equals("/piania/announcements"))) {
            return chain.filter(exchange);
        }

        String authHeader =
                exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Missing Authorization header",
                    HttpStatus.UNAUTHORIZED);
        }

        try {

            String token = authHeader.substring(7);

            Claims claims = jwtService.validateToken(token);

            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            // ✅ Propagar headers internos
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> r.headers(headers -> {
                        headers.add("X-User-Email", email);
                        headers.add("X-User-Role", role);
                    }))
                    .build();

            return chain.filter(mutatedExchange);

        } catch (Exception e) {
            log.warn("JWT validation failed: {} - {}", e.getClass().getName(), e.getMessage(), e);
            return onError(exchange, "Invalid or expired JWT",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange,
                               String message,
                               HttpStatus status) {

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);

        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(bytes))
        );
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
