package com.skillsync.controller;

import com.skillsync.dto.project.CreateProjectRequest;
import com.skillsync.dto.project.ProjectResponse;
import com.skillsync.dto.project.UpdateProjectRequest;
import com.skillsync.dto.team.TeamResponse;
import com.skillsync.enums.ProjectStatus;
import com.skillsync.security.UserPrincipal;
import com.skillsync.service.ProjectService;
import com.skillsync.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Campus project creation, vacancy discovery, and team management")
public class ProjectController {

    private final ProjectService projectService;
    private final TeamService teamService;

    public ProjectController(ProjectService projectService, TeamService teamService) {
        this.projectService = projectService;
        this.teamService = teamService;
    }

    @PostMapping
    @Operation(summary = "Create a project", description = "Creates a project, its working team, and automatically designates the creator as the initial team leader.")
    public ResponseEntity<ProjectResponse> createProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        ProjectResponse response = projectService.createProject(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List and search projects", description = "Browse open campus projects with optional status, skill, and keyword search filters.")
    public ResponseEntity<Page<ProjectResponse>> listProjects(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) Long skillId,
            @RequestParam(required = false) String query,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        Page<ProjectResponse> page = projectService.listProjects(status, skillId, query, currentUserId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project details", description = "Retrieve full project description, required skills, and team vacancy info.")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        ProjectResponse response = projectService.getProject(id, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project", description = "Update project title, description, status, or max team size (Project owner only).")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        ProjectResponse updated = projectService.updateProject(id, principal.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project", description = "Permanently removes a project and associated team records (Project owner only).")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        projectService.deleteProject(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/team")
    @Operation(summary = "Get project team roster", description = "View team members and roles for a project.")
    public ResponseEntity<TeamResponse> getProjectTeam(@PathVariable Long id) {
        TeamResponse team = teamService.getTeamByProjectId(id);
        return ResponseEntity.ok(team);
    }
}
