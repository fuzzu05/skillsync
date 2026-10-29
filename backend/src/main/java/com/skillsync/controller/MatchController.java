package com.skillsync.controller;

import com.skillsync.dto.match.CandidateMatchResponse;
import com.skillsync.dto.match.ProjectMatchResponse;
import com.skillsync.security.UserPrincipal;
import com.skillsync.service.SkillMatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matches")
@Tag(name = "Skill Matchmaking", description = "Explainable recommendation engine for projects and collaborators")
public class MatchController {

    private final SkillMatchingService matchingService;

    public MatchController(SkillMatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @GetMapping("/projects")
    @Operation(summary = "Recommend projects for me", description = "Returns open campus projects ranked by explainable skill compatibility with match score breakdowns.")
    public ResponseEntity<List<ProjectMatchResponse>> getRecommendedProjects(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        List<ProjectMatchResponse> recommendations = matchingService.recommendProjectsForUser(principal.getId(), limit);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/projects/{projectId}/candidates")
    @Operation(summary = "Recommend candidates for a project", description = "Finds eligible student collaborators ranked by proficiency in the project's required skills.")
    public ResponseEntity<List<CandidateMatchResponse>> getRecommendedCandidates(
            @PathVariable Long projectId,
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        List<CandidateMatchResponse> candidates = matchingService.recommendCandidatesForProject(projectId, limit);
        return ResponseEntity.ok(candidates);
    }
}
