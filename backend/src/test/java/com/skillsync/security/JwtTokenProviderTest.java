package com.skillsync.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(secret, expirationMs);
    }

    @Test
    @DisplayName("Should generate valid JWT and correctly extract claims")
    void testGenerateAndValidateToken() {
        Long userId = 42L;
        String email = "student@university.edu";
        String role = "ROLE_STUDENT";

        String token = tokenProvider.generateTokenFromUser(userId, email, role);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUserIdFromJWT(token)).isEqualTo(42L);
        assertThat(tokenProvider.getEmailFromJWT(token)).isEqualTo("student@university.edu");
    }

    @Test
    @DisplayName("Should reject tampered or invalid JWT token")
    void testRejectTamperedToken() {
        String token = tokenProvider.generateTokenFromUser(1L, "test@university.edu", "ROLE_STUDENT");
        String tamperedToken = token + "corrupted";

        assertThat(tokenProvider.validateToken(tamperedToken)).isFalse();
    }
}
