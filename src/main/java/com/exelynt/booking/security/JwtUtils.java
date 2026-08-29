package com.exelynt.booking.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    // Must match the dev-only fallback in application.properties (jwt.secret).
    // If this exact value is ever detected outside a dev profile, startup fails
    // rather than silently signing tokens with a publicly-known key.
    private static final String DEFAULT_DEV_SECRET =
            "dev-only-9a2f8c4e7b1d3f6a8c0e2b4d6f8a1c3e5b7d9f2a4c6e8b0d2f4a6c8e0b2d4f6a";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    private final Environment environment;

    public JwtUtils(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateSecret() {
        boolean isProdProfile = java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (isProdProfile && DEFAULT_DEV_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "JWT_SECRET environment variable is not set. Refusing to start the 'prod' " +
                    "profile with the publicly-known default dev secret.");
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}