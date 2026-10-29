package com.skillsync.service;

import com.skillsync.dto.match.CandidateMatchResponse;
import com.skillsync.dto.match.ProjectMatchResponse;
import com.skillsync.dto.match.SkillMatchDetail;
import com.skillsync.entity.*;
import com.skillsync.enums.ProjectStatus;
import com.skillsync.exception.ResourceNotFoundException;
import com.skillsync.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SkillMatchingService {

    private final ProjectRepository projectRepository;
    private final ProjectSkillRepository projectSkillRepository;
    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final TeamMemberRepository teamMemberRepository;

    public SkillMatchingService(ProjectRepository projectRepository,
                                ProjectSkillRepository projectSkillRepository,
                                UserRepository userRepository,
                                UserSkillRepository userSkillRepository,
                                TeamMemberRepository teamMemberRepository) {
        this.projectRepository = projectRepository;
        this.projectSkillRepository = projectSkillRepository;
        this.userRepository = userRepository;
        this.userSkillRepository = userSkillRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    /**
     * Recommend open projects to a student based on explainable skill matching.
     */
    @Transactional(readOnly = true)
    public List<ProjectMatchResponse> recommendProjectsForUser(Long userId, int limit) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<UserSkill> userSkills = userSkillRepository.findByUserIdWithSkill(userId);
        Map<Long, UserSkill> userSkillMap = new HashMap<>();
        for (UserSkill us : userSkills) {
            userSkillMap.put(us.getSkill().getId(), us);
        }

        List<Project> openProjects = projectRepository.findByStatus(ProjectStatus.OPEN, PageRequest.of(0, 100)).getContent();

        List<ProjectMatchResponse> recommendations = new ArrayList<>();

        for (Project project : openProjects) {
            // Skip projects the student already owns or is a member of
            if (project.getOwner().getId().equals(userId)) {
                continue;
            }
            if (project.getTeam() != null && teamMemberRepository.existsByIdTeamIdAndIdUserId(project.getTeam().getId(), userId)) {
                continue;
            }

            List<ProjectSkill> requiredSkills = projectSkillRepository.findByProjectIdWithSkill(project.getId());
            if (requiredSkills.isEmpty()) {
                continue; // Skip projects with no skill criteria
            }

            MatchResult result = calculateMatch(requiredSkills, userSkillMap);

            long teamSize = project.getTeam() != null ? teamMemberRepository.countByIdTeamId(project.getTeam().getId()) : 0;

            recommendations.add(new ProjectMatchResponse(
                    project.getId(),
                    project.getTitle(),
                    project.getSummary(),
                    project.getStatus(),
                    project.getMaxTeamSize(),
                    teamSize,
                    result.score(),
                    (int) Math.round(result.score()),
                    result.matchedSkills(),
                    result.missingSkills(),
                    result.isFullyQualified()
            ));
        }

        // Sort descending by match score, prioritizing fully qualified candidates
        recommendations.sort(Comparator.comparingDouble(ProjectMatchResponse::matchScore).reversed());

        return recommendations.stream().limit(Math.max(1, limit)).toList();
    }

    /**
     * Recommend student candidates for a project based on required skills.
     */
    @Transactional(readOnly = true)
    public List<CandidateMatchResponse> recommendCandidatesForProject(Long projectId, int limit) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        List<ProjectSkill> requiredSkills = projectSkillRepository.findByProjectIdWithSkill(projectId);
        if (requiredSkills.isEmpty()) {
            return List.of();
        }

        List<User> allUsers = userRepository.findAll();
        List<CandidateMatchResponse> candidates = new ArrayList<>();

        for (User candidate : allUsers) {
            // Exclude owner and existing members
            if (candidate.getId().equals(project.getOwner().getId())) {
                continue;
            }
            if (project.getTeam() != null && teamMemberRepository.existsByIdTeamIdAndIdUserId(project.getTeam().getId(), candidate.getId())) {
                continue;
            }

            List<UserSkill> candidateSkills = userSkillRepository.findByUserIdWithSkill(candidate.getId());
            Map<Long, UserSkill> skillMap = new HashMap<>();
            for (UserSkill us : candidateSkills) {
                skillMap.put(us.getSkill().getId(), us);
            }

            MatchResult result = calculateMatch(requiredSkills, skillMap);

            // Filter out candidates with 0% match to ensure high quality results
            if (result.score() > 0.0) {
                candidates.add(new CandidateMatchResponse(
                        candidate.getId(),
                        candidate.getFullName(),
                        candidate.getHeadline(),
                        candidate.getDepartment(),
                        candidate.getYearOfStudy(),
                        candidate.getGithubUrl(),
                        candidate.getLinkedinUrl(),
                        result.score(),
                        (int) Math.round(result.score()),
                        result.matchedSkills(),
                        result.missingSkills(),
                        result.isFullyQualified()
                ));
            }
        }

        candidates.sort(Comparator.comparingDouble(CandidateMatchResponse::matchScore).reversed());

        return candidates.stream().limit(Math.max(1, limit)).toList();
    }

    private MatchResult calculateMatch(List<ProjectSkill> requiredSkills, Map<Long, UserSkill> userSkillMap) {
        double totalPossibleScore = 0.0;
        double totalEarnedScore = 0.0;
        boolean allMandatorySatisfied = true;

        List<SkillMatchDetail> matched = new ArrayList<>();
        List<SkillMatchDetail> missing = new ArrayList<>();

        for (ProjectSkill req : requiredSkills) {
            double multiplier = req.isMandatory() ? 1.5 : 1.0;
            double maxSkillScore = req.getMinProficiency().getWeight() * multiplier;
            totalPossibleScore += maxSkillScore;

            UserSkill userSkill = userSkillMap.get(req.getSkill().getId());

            if (userSkill != null) {
                int userWeight = userSkill.getProficiency().getWeight();
                int reqWeight = req.getMinProficiency().getWeight();
                boolean isSatisfied = userWeight >= reqWeight;

                double ratio = Math.min(1.0, (double) userWeight / (double) reqWeight);
                totalEarnedScore += maxSkillScore * ratio;

                SkillMatchDetail detail = new SkillMatchDetail(
                        req.getSkill().getId(),
                        req.getSkill().getName(),
                        req.getSkill().getCategory(),
                        req.getMinProficiency(),
                        userSkill.getProficiency(),
                        isSatisfied,
                        req.isMandatory()
                );

                if (isSatisfied) {
                    matched.add(detail);
                } else {
                    missing.add(detail);
                    if (req.isMandatory()) {
                        allMandatorySatisfied = false;
                    }
                }
            } else {
                if (req.isMandatory()) {
                    allMandatorySatisfied = false;
                }
                missing.add(new SkillMatchDetail(
                        req.getSkill().getId(),
                        req.getSkill().getName(),
                        req.getSkill().getCategory(),
                        req.getMinProficiency(),
                        null,
                        false,
                        req.isMandatory()
                ));
            }
        }

        double score = totalPossibleScore > 0 ? (totalEarnedScore / totalPossibleScore) * 100.0 : 100.0;
        // Cap score at 100.0%
        score = Math.min(100.0, Math.round(score * 10.0) / 10.0);

        return new MatchResult(score, matched, missing, allMandatorySatisfied);
    }

    private record MatchResult(
            double score,
            List<SkillMatchDetail> matchedSkills,
            List<SkillMatchDetail> missingSkills,
            boolean isFullyQualified
    ) {}
}
