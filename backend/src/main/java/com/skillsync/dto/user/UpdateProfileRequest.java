package com.skillsync.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Full name cannot be blank")
        @Size(max = 100, message = "Full name cannot exceed 100 characters")
        String fullName,

        @Size(max = 200, message = "Headline cannot exceed 200 characters")
        String headline,

        String bio,

        @Size(max = 100, message = "Department cannot exceed 100 characters")
        String department,

        @Min(value = 1, message = "Year of study must be between 1 and 5")
        @Max(value = 5, message = "Year of study must be between 1 and 5")
        Integer yearOfStudy,

        @Size(max = 255, message = "GitHub URL cannot exceed 255 characters")
        String githubUrl,

        @Size(max = 255, message = "LinkedIn URL cannot exceed 255 characters")
        String linkedinUrl
) {
}
