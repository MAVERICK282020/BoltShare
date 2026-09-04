package com.localsync.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class JwtService {

    @Value("${localsync.jwt.secret}")
    private String secret;

    @Value("${localsync.jwt.expiration}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
            java.util.Base64.getEncoder().encodeToString(secret.getBytes())
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String deviceId, String deviceName, Map<String, Object> extraClaims) {
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
            .id(jti)
            .subject(deviceId)
            .claim("deviceName", deviceName)
            .claims(extraClaims)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    public String generateTemporaryToken(String deviceId, String deviceName, long ttlMs) {
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
            .id(jti)
            .subject(deviceId)
            .claim("deviceName", deviceName)
            .claim("temporary", true)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + ttlMs))
            .signWith(getSigningKey())
            .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String extractDeviceId(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }

    public String extractDeviceName(String token) {
        return extractAllClaims(token).get("deviceName", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}
