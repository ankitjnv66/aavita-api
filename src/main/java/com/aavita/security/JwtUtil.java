package com.aavita.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class JwtUtil {

    private final JwtProperties properties;
    private SecretKey key;
    private long expirationMs;

    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (properties.getKey() == null || properties.getKey().isBlank()) {
            throw new IllegalStateException("JWT key must not be empty");
        }

        byte[] decodedKey = Base64.getDecoder().decode(properties.getKey());
        this.key = Keys.hmacShaKeyFor(decodedKey);
        this.expirationMs = properties.getExpirationHours() * 60L * 60 * 1000;
    }

    public String generateToken(String email, Long userId, String fullName, String userType) {

        String issuer = getPrimary(properties.getIssuers(), "aavita-api");
        String audience = getPrimary(properties.getAudiences(), "aavita-api");

        return Jwts.builder()
                .subject(email)
                .claim("user_id", userId)
                .claim("name", fullName)
                .claim("userType", userType)
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Claims validateToken(String token) {

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        validateIssuer(claims);
        validateAudience(claims);

        return claims;
    }

    public String getEmailFromToken(String token) {
        return validateToken(token).getSubject();
    }

    private void validateIssuer(Claims claims) {
        List<String> allowedIssuers = properties.getIssuers();
        if (allowedIssuers != null && !allowedIssuers.isEmpty()) {
            if (!allowedIssuers.contains(claims.getIssuer())) {
                throw new JwtException("Invalid JWT issuer");
            }
        }
    }

    private void validateAudience(Claims claims) {
        List<String> allowedAudiences = properties.getAudiences();
        if (allowedAudiences != null && !allowedAudiences.isEmpty()) {
            Set<String> tokenAudiences = claims.getAudience();  // returns Set<String>
            boolean valid = tokenAudiences.stream()
                    .anyMatch(allowedAudiences::contains);
            if (!valid) {
                throw new JwtException("Invalid JWT audience");
            }
        }
    }

    private String getPrimary(List<String> values, String defaultValue) {
        return (values == null || values.isEmpty()) ? defaultValue : values.get(0);
    }
}
