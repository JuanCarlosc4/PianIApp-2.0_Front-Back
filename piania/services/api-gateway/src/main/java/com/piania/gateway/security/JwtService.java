package com.piania.gateway.security;

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

    public Claims validateToken(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .requireIssuer(expectedIssuer)
                .requireAudience(expectedAudience)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
