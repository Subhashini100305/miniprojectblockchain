package com.miniproject.verificationApp.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String ISSUER = "tourism-review-system";
    private static final String AUDIENCE = "tourism-review-frontend";
    private static final int TOKEN_VERSION = 1;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private Key getSigningKey() {
        return new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                SignatureAlgorithm.HS256.getJcaName()
        );
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuer(ISSUER)
                .id(UUID.randomUUID().toString())
                .audience().add(AUDIENCE).and()
                .claim("ver", TOKEN_VERSION)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractTokenId(String token) {
        return parseClaims(token).getId();
    }

    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            Integer version = claims.get("ver", Integer.class);

            return ISSUER.equals(claims.getIssuer())
                    && claims.getAudience() != null
                    && claims.getAudience().contains(AUDIENCE)
                    && Integer.valueOf(TOKEN_VERSION).equals(version)
                    && claims.getId() != null
                    && !claims.getId().isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
