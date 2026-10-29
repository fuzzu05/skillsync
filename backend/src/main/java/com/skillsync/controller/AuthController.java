package com.skillsync.controller;

import com.skillsync.dto.auth.AuthResponse;
import com.skillsync.dto.auth.LoginRequest;
import com.skillsync.dto.auth.RegisterRequest;
import com.skillsync.dto.user.UserProfileResponse;
import com.skillsync.security.UserPrincipal;
import com.skillsync.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for student registration, login, and token verification")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new student", description = "Creates a new student account, hashes the password with BCrypt, and returns a JWT access token.")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates student credentials and returns a JWT Bearer token.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated profile", description = "Returns full profile and skill list for the currently logged in student.")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponse profile = authService.getCurrentUserProfile(principal);
        return ResponseEntity.ok(profile);
    }
}
