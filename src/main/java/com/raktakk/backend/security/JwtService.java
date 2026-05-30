package com.raktakk.backend.security;

import com.raktakk.backend.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${app.jwt.refresh-secret}")
    private String refreshSecret;

    public String generateAccessToken(String email, Role role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessExpirationMs);
        return Jwts.builder()
                .subject(email)
                .id(UUID.randomUUID().toString())
                .claim("role", role.name())
                .claim("typ", "access")
                .issuedAt(now)
                .expiration(exp)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshExpirationMs);
        return Jwts.builder()
                .subject(email)
                .id(UUID.randomUUID().toString())
                .claim("typ", "refresh")
                .issuedAt(now)
                .expiration(exp)
                .signWith(getRefreshSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractUsernameFromRefresh(String token) {
        return extractAllClaimsRefresh(token).getSubject();
    }

    public boolean isTokenValid(String token, String username) {
        Claims claims = extractAllClaims(token);
        return username.equals(claims.getSubject())
                && claims.getExpiration().after(new Date())
                && "access".equals(claims.get("typ", String.class));
    }

    public boolean isRefreshTokenValid(String token, String username) {
        Claims claims = extractAllClaimsRefresh(token);
        return username.equals(claims.getSubject())
                && claims.getExpiration().after(new Date())
                && "refresh".equals(claims.get("typ", String.class));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Claims extractAllClaimsRefresh(String token) {
        return Jwts.parser()
                .verifyWith(getRefreshSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return toSigningKey(secret, "JWT_SECRET");
    }

    private SecretKey getRefreshSigningKey() {
        return toSigningKey(refreshSecret, "JWT_REFRESH_SECRET");
    }

    private SecretKey toSigningKey(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " is not configured");
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(value);
        } catch (IllegalArgumentException ex) {
            keyBytes = value.getBytes(StandardCharsets.UTF_8);
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
