package com.skillsync.controller;

import com.skillsync.dto.skill.CreateSkillRequest;
import com.skillsync.dto.skill.SkillResponse;
import com.skillsync.enums.SkillCategory;
import com.skillsync.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/skills")
@Tag(name = "Skills", description = "Skill directory and classification management")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    @Operation(summary = "List skills", description = "Fetch all skills with optional filtering by discipline category or keyword search.")
    public ResponseEntity<List<SkillResponse>> getAllSkills(
            @RequestParam(required = false) SkillCategory category,
            @RequestParam(required = false) String query
    ) {
        List<SkillResponse> skills = skillService.getAllSkills(category, query);
        return ResponseEntity.ok(skills);
    }

    @PostMapping
    @Operation(summary = "Add a new skill", description = "Register a new skill in the campus taxonomy.")
    public ResponseEntity<SkillResponse> createSkill(@Valid @RequestBody CreateSkillRequest request) {
        SkillResponse created = skillService.createSkill(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
