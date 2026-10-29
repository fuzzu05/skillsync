package com.skillsync.dto.auth;

import com.skillsync.enums.Role;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Long userId,
        String email,
        String fullName,
        Role role
) {
    public static AuthResponse of(String token, Long userId, String email, String fullName, Role role) {
        return new AuthResponse(token, "Bearer", userId, email, fullName, role);
    }
}
