package com.piania.core.security;

import java.security.Key;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.issuer}")
    private String expectedIssuer;

    @Value("${jwt.audience}")
    private String expectedAudience;

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    private Claims extractAllClaims(String token) {

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .requireIssuer(expectedIssuer)
                .requireAudience(expectedAudience)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims;
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractAllClaims(token)
                .get("role", String.class);
    }
}