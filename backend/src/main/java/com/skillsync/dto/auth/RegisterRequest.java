package com.skillsync.dto.auth;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email cannot exceed 150 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name cannot exceed 100 characters")
        String fullName,

        @Size(max = 100, message = "Department cannot exceed 100 characters")
        String department,

        @Min(value = 1, message = "Year of study must be between 1 and 5")
        @Max(value = 5, message = "Year of study must be between 1 and 5")
        Integer yearOfStudy
) {
}
