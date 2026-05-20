package com.piania.auth.security;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.piania.auth.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access.expiration}")
    private long accessExpiration;

    // Generar token
    public String generateToken(User user) {

    	return Jwts.builder()
    	        .setSubject(user.getEmail())
    	        .claim("role", user.getRole().name())
    	        .setIssuer("piania-auth-service")
    	        .setAudience("piania-services")
    	        .setIssuedAt(new Date())
    	        .setExpiration(
    	                new Date(System.currentTimeMillis() + accessExpiration)
    	        )
    	        .signWith(getSignKey(), SignatureAlgorithm.HS256)
    	        .compact();
    }

    // Obtener username (subject)
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Método genérico profesional para extraer claims
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Validar token
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) 
                && !isTokenExpired(token);
    }

    // Verificar expiración
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extraer todos los claims
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Generar clave segura
    private Key getSignKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
}
