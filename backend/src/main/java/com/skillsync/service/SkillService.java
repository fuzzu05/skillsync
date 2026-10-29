package com.skillsync.service;

import com.skillsync.dto.skill.CreateSkillRequest;
import com.skillsync.dto.skill.SkillResponse;
import com.skillsync.entity.Skill;
import com.skillsync.enums.SkillCategory;
import com.skillsync.exception.DuplicateResourceException;
import com.skillsync.repository.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getAllSkills(SkillCategory category, String query) {
        if (query != null && !query.isBlank()) {
            return skillRepository.searchSkills(query.trim()).stream()
                    .map(SkillResponse::fromEntity)
                    .toList();
        }
        if (category != null) {
            return skillRepository.findByCategory(category).stream()
                    .map(SkillResponse::fromEntity)
                    .toList();
        }
        return skillRepository.findAll().stream()
                .map(SkillResponse::fromEntity)
                .toList();
    }

    @Transactional
    public SkillResponse createSkill(CreateSkillRequest request) {
        if (skillRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new DuplicateResourceException("Skill with name '" + request.name() + "' already exists");
        }

        Skill skill = new Skill(
                request.name().trim(),
                request.category(),
                request.description() != null ? request.description().trim() : null
        );

        Skill saved = skillRepository.save(skill);
        return SkillResponse.fromEntity(saved);
    }
}
