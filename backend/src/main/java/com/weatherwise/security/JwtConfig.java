package com.weatherwise.security;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
public class JwtConfig {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @Bean
    public SecretKey jwtSecretKey() {
        boolean production = Arrays.stream(activeProfiles.split(","))
                .anyMatch(profile -> profile.trim().equalsIgnoreCase("prod"));

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET is required and must be configured before starting Roamora."
            );
        }

        if (production && (secret.length() < 32 || secret.toLowerCase().contains("change-me")
                || secret.toLowerCase().contains("development-secret"))) {
            throw new IllegalStateException(
                    "Production requires a strong random APP_JWT_SECRET of at least 32 characters; placeholder secrets are forbidden."
            );
        }

        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}