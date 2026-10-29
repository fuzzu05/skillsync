package com.skillsync.controller;

import com.skillsync.dto.user.UpdateProfileRequest;
import com.skillsync.dto.user.UserProfileResponse;
import com.skillsync.dto.user.UserSkillRequest;
import com.skillsync.dto.user.UserSkillResponse;
import com.skillsync.security.UserPrincipal;
import com.skillsync.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Student profiles, skill selections, and talent directory search")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get student profile by ID", description = "Retrieve public profile information, bio, and skills for any registered student.")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long id) {
        UserProfileResponse profile = userService.getUserProfile(id);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update personal profile", description = "Modify full name, headline, bio, department, and social links.")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserProfileResponse updated = userService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/skills")
    @Operation(summary = "Update personal skills", description = "Configure skills, proficiency levels (BEGINNER, INTERMEDIATE, ADVANCED, EXPERT), and years of experience.")
    public ResponseEntity<List<UserSkillResponse>> updateUserSkills(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody List<UserSkillRequest> requests
    ) {
        List<UserSkillResponse> updatedSkills = userService.updateUserSkills(principal.getId(), requests);
        return ResponseEntity.ok(updatedSkills);
    }

    @GetMapping
    @Operation(summary = "Search student talent", description = "Search students by keyword across name, headline, and department with pagination.")
    public ResponseEntity<Page<UserProfileResponse>> searchUsers(
            @RequestParam(required = false, defaultValue = "") String query,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<UserProfileResponse> users = userService.searchUsers(query, pageable);
        return ResponseEntity.ok(users);
    }
}
