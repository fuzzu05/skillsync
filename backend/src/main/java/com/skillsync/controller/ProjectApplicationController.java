package com.skillsync.controller;

import com.skillsync.dto.application.ApplyProjectRequest;
import com.skillsync.dto.application.ProjectApplicationResponse;
import com.skillsync.dto.application.ReviewApplicationRequest;
import com.skillsync.enums.ApplicationStatus;
import com.skillsync.security.UserPrincipal;
import com.skillsync.service.ProjectApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Project Applications", description = "Application workflow for joining campus project teams")
public class ProjectApplicationController {

    private final ProjectApplicationService applicationService;

    public ProjectApplicationController(ProjectApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/projects/{projectId}/apply")
    @Operation(summary = "Apply to join a project", description = "Submit an application with a pitch. Enforces single application per student and capacity check.")
    public ResponseEntity<ProjectApplicationResponse> applyToProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ApplyProjectRequest request
    ) {
        ProjectApplicationResponse response = applicationService.applyToProject(projectId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projects/{projectId}/applications")
    @Operation(summary = "View project applications", description = "Project owner reviews submitted student applications with optional status filter.")
    public ResponseEntity<List<ProjectApplicationResponse>> getProjectApplications(
            @PathVariable Long projectId,
            @RequestParam(required = false) ApplicationStatus status,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ProjectApplicationResponse> applications = applicationService.getProjectApplications(projectId, principal.getId(), status);
        return ResponseEntity.ok(applications);
    }

    @PatchMapping("/projects/{projectId}/applications/{applicationId}")
    @Operation(summary = "Review project application", description = "Project owner accepts or rejects an application. Concurrency-safe team capacity is enforced.")
    public ResponseEntity<ProjectApplicationResponse> reviewApplication(
            @PathVariable Long projectId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewApplicationRequest request
    ) {
        ProjectApplicationResponse response = applicationService.reviewApplication(
                projectId, applicationId, principal.getId(), request
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/projects/{projectId}/applications")
    @Operation(summary = "Withdraw application", description = "Student withdraws their pending application before owner review.")
    public ResponseEntity<Void> withdrawApplication(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        applicationService.withdrawApplication(projectId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/applications/my-applications")
    @Operation(summary = "Get my applications", description = "Logged-in student views all project applications they have submitted.")
    public ResponseEntity<List<ProjectApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ProjectApplicationResponse> applications = applicationService.getMyApplications(principal.getId());
        return ResponseEntity.ok(applications);
    }
}
