package com.skillsync.service;

import com.skillsync.dto.match.CandidateMatchResponse;
import com.skillsync.dto.match.ProjectMatchResponse;
import com.skillsync.entity.*;
import com.skillsync.enums.Proficiency;
import com.skillsync.enums.ProjectStatus;
import com.skillsync.enums.SkillCategory;
import com.skillsync.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillMatchingServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectSkillRepository projectSkillRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSkillRepository userSkillRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private SkillMatchingService matchingService;

    private User student;
    private User owner;
    private Project project;
    private Skill javaSkill;
    private Skill reactSkill;

    @BeforeEach
    void setUp() {
        student = new User("alice@university.edu", "hash", "Alice Smith", "CS", 3);
        student.setId(10L);

        owner = new User("bob@university.edu", "hash", "Bob Jones", "CS", 4);
        owner.setId(20L);

        project = new Project(owner, "Campus Food App", "Mobile app for food delivery", "Description", 4);
        project.setId(100L);

        javaSkill = new Skill("Java", SkillCategory.BACKEND, "Java programming");
        javaSkill.setId(1L);

        reactSkill = new Skill("React", SkillCategory.FRONTEND, "React framework");
        reactSkill.setId(2L);
    }

    @Test
    @DisplayName("Should recommend projects with accurate match score and skill breakdown")
    void testRecommendProjectsForUser() {
        // Arrange
        // Project requires: Java (ADVANCED, mandatory), React (INTERMEDIATE, optional)
        ProjectSkill req1 = new ProjectSkill(project, javaSkill, Proficiency.ADVANCED, true);
        ProjectSkill req2 = new ProjectSkill(project, reactSkill, Proficiency.INTERMEDIATE, false);
        List<ProjectSkill> requiredSkills = List.of(req1, req2);

        // Student has: Java (ADVANCED)
        UserSkill studentJava = new UserSkill(student, javaSkill, Proficiency.ADVANCED, BigDecimal.valueOf(2.0));
        List<UserSkill> studentSkills = List.of(studentJava);

        when(userRepository.findById(10L)).thenReturn(Optional.of(student));
        when(userSkillRepository.findByUserIdWithSkill(10L)).thenReturn(studentSkills);
        when(projectRepository.findByStatus(eq(ProjectStatus.OPEN), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(project)));
        when(projectSkillRepository.findByProjectIdWithSkill(100L)).thenReturn(requiredSkills);

        // Act
        List<ProjectMatchResponse> recommendations = matchingService.recommendProjectsForUser(10L, 5);

        // Assert
        assertThat(recommendations).hasSize(1);
        ProjectMatchResponse match = recommendations.get(0);
        assertThat(match.projectId()).isEqualTo(100L);
        assertThat(match.matchedSkills()).hasSize(1);
        assertThat(match.matchedSkills().get(0).skillName()).isEqualTo("Java");
        assertThat(match.missingSkills()).hasSize(1);
        assertThat(match.missingSkills().get(0).skillName()).isEqualTo("React");
        // Java was mandatory and satisfied, so isFullyQualified is true because React is optional
        assertThat(match.isFullyQualified()).isTrue();
        assertThat(match.matchScore()).isGreaterThan(50.0);
    }

    @Test
    @DisplayName("Should recommend candidates for project matching required skills")
    void testRecommendCandidatesForProject() {
        // Arrange
        ProjectSkill req = new ProjectSkill(project, javaSkill, Proficiency.INTERMEDIATE, true);
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(projectSkillRepository.findByProjectIdWithSkill(100L)).thenReturn(List.of(req));

        User candidate = new User("charlie@university.edu", "hash", "Charlie Brown", "IT", 2);
        candidate.setId(30L);
        when(userRepository.findAll()).thenReturn(List.of(candidate));

        UserSkill charlieSkill = new UserSkill(candidate, javaSkill, Proficiency.ADVANCED, BigDecimal.valueOf(1.5));
        when(userSkillRepository.findByUserIdWithSkill(30L)).thenReturn(List.of(charlieSkill));

        // Act
        List<CandidateMatchResponse> candidates = matchingService.recommendCandidatesForProject(100L, 5);

        // Assert
        assertThat(candidates).hasSize(1);
        CandidateMatchResponse candidateMatch = candidates.get(0);
        assertThat(candidateMatch.candidateId()).isEqualTo(30L);
        assertThat(candidateMatch.matchPercentage()).isEqualTo(100);
        assertThat(candidateMatch.isFullyQualified()).isTrue();
    }
}
