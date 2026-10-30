package com.weatherwise.security;

import com.weatherwise.auth.RefreshToken;
import com.weatherwise.auth.RefreshTokenRepository;
import com.weatherwise.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {
    private RefreshTokenRepository repository;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        repository = mock(RefreshTokenRepository.class);
        service = new RefreshTokenService(repository, 30);
    }

    @Test
    void rotateRevokesOldTokenAndIssuesReplacement() {
        User user = new User();
        RefreshToken old = new RefreshToken();
        old.setToken("existing-refresh-token");
        old.setUser(user);
        old.setExpiresAt(LocalDateTime.now().plusDays(2));
        old.setRevoked(false);
        when(repository.findByTokenForUpdate("existing-refresh-token")).thenReturn(Optional.of(old));
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String replacement = service.rotate("existing-refresh-token", user);

        assertNotNull(replacement);
        assertNotEquals("existing-refresh-token", replacement);
        assertTrue(old.isRevoked(), "the previous refresh token must be revoked");
        verify(repository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void rejectExpiredRefreshToken() {
        RefreshToken expired = new RefreshToken();
        expired.setToken("expired-token");
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(repository.findByTokenForUpdate("expired-token")).thenReturn(Optional.of(expired));

        assertThrows(IllegalArgumentException.class, () -> service.validate("expired-token"));
        verify(repository, never()).save(any(RefreshToken.class));
    }

    @Test
    void rejectRevokedRefreshToken() {
        RefreshToken revoked = new RefreshToken();
        revoked.setToken("revoked-token");
        revoked.setExpiresAt(LocalDateTime.now().plusDays(1));
        revoked.setRevoked(true);
        when(repository.findByTokenForUpdate("revoked-token")).thenReturn(Optional.of(revoked));

        assertThrows(IllegalArgumentException.class, () -> service.validate("revoked-token"));
    }
}
