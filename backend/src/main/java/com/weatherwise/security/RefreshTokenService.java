package com.weatherwise.security;

import com.weatherwise.auth.RefreshToken;
import com.weatherwise.auth.RefreshTokenRepository;
import com.weatherwise.auth.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long expirationDays;

    private final SecureRandom secureRandom =
            new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-expiration-days:30}")
            long expirationDays
    ) {
        this.refreshTokenRepository =
                refreshTokenRepository;

        this.expirationDays =
                expirationDays;
    }

    public String createRefreshToken(User user) {

        String tokenValue =
                generateToken();

        RefreshToken refreshToken =
                new RefreshToken();

        refreshToken.setToken(tokenValue);
        refreshToken.setUser(user);

        refreshToken.setExpiresAt(
                LocalDateTime.now()
                        .plusDays(expirationDays)
        );

        refreshToken.setRevoked(false);

        refreshTokenRepository.save(
                refreshToken
        );

        return tokenValue;
    }

    public RefreshToken validate(
            String token
    ) {

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByToken(token)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid refresh token"
                                )
                        );

        if (refreshToken.isRevoked()) {
            throw new IllegalArgumentException(
                    "Refresh token has been revoked"
            );
        }

        if (refreshToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Refresh token has expired"
            );
        }

        return refreshToken;
    }

    public void revoke(
            String token
    ) {

        refreshTokenRepository
                .findByToken(token)
                .ifPresent(refreshToken -> {

                    refreshToken.setRevoked(true);

                    refreshTokenRepository.save(
                            refreshToken
                    );
                });
    }

    public void revokeAllForUser(
            Long userId
    ) {

        refreshTokenRepository
                .deleteAllByUserId(userId);
    }

    private String generateToken() {

        byte[] bytes = new byte[64];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}