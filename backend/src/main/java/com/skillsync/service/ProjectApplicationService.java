package com.skillsync.service;

import com.skillsync.dto.application.ApplyProjectRequest;
import com.skillsync.dto.application.ProjectApplicationResponse;
import com.skillsync.dto.application.ReviewApplicationRequest;
import com.skillsync.dto.user.UserSkillResponse;
import com.skillsync.entity.*;
import com.skillsync.enums.ApplicationStatus;
import com.skillsync.enums.NotificationType;
import com.skillsync.enums.ProjectStatus;
import com.skillsync.enums.TeamRole;
import com.skillsync.exception.AccessDeniedException;
import com.skillsync.exception.BadRequestException;
import com.skillsync.exception.DuplicateResourceException;
import com.skillsync.exception.ResourceNotFoundException;
import com.skillsync.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ProjectApplicationService {

    private final ProjectRepository projectRepository;
    private final ProjectApplicationRepository applicationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final NotificationService notificationService;

    public ProjectApplicationService(ProjectRepository projectRepository,
                                     ProjectApplicationRepository applicationRepository,
                                     TeamMemberRepository teamMemberRepository,
                                     UserRepository userRepository,
                                     UserSkillRepository userSkillRepository,
                                     NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.applicationRepository = applicationRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.userSkillRepository = userSkillRepository;
        this.notificationService = notificationService;
    }

    /**
     * Rule: Allow only one application per student per project, even after withdrawal or rejection.
     */
    @Transactional
    public ProjectApplicationResponse applyToProject(Long projectId, Long applicantId, ApplyProjectRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", applicantId));

        // 1. Owner cannot apply to their own project
        if (project.getOwner().getId().equals(applicantId)) {
            throw new BadRequestException("You cannot apply to your own project");
        }

        // 2. Strict Rule: Allow only one application per student per project, even after withdrawal or rejection
        if (applicationRepository.existsByProjectIdAndApplicantId(projectId, applicantId)) {
            throw new DuplicateResourceException("You have already submitted an application for this project. Only one application per student per project is permitted.");
        }

        // 3. Project status must be OPEN
        if (project.getStatus() != ProjectStatus.OPEN) {
            throw new BadRequestException("Applications are closed for this project (status: " + project.getStatus() + ")");
        }

        // 4. Must not already be a team member
        if (project.getTeam() != null && teamMemberRepository.existsByIdTeamIdAndIdUserId(project.getTeam().getId(), applicantId)) {
            throw new BadRequestException("You are already an active member of this project's team");
        }

        // 5. Team capacity check
        if (project.getTeam() != null) {
            long currentCount = teamMemberRepository.countByIdTeamId(project.getTeam().getId());
            if (currentCount >= project.getMaxTeamSize()) {
                throw new BadRequestException("Project team is already at full capacity (" + project.getMaxTeamSize() + " members)");
            }
        }

        ProjectApplication application = new ProjectApplication(project, applicant, request.pitch().trim());
        ProjectApplication saved = applicationRepository.save(application);

        // Trigger notification to project owner
        notificationService.createNotification(
                project.getOwner(),
                applicant,
                NotificationType.PROJECT_APPLICATION,
                "New Project Application",
                applicant.getFullName() + " applied to join '" + project.getTitle() + "'",
                project.getId()
        );

        List<UserSkillResponse> applicantSkills = userSkillRepository.findByUserIdWithSkill(applicantId)
                .stream()
                .map(UserSkillResponse::fromEntity)
                .toList();

        return ProjectApplicationResponse.fromEntity(saved, applicantSkills);
    }

    /**
     * Rule: Enforce team capacity safely during concurrent application acceptances.
     * Uses pessimistic write lock on the project to serialize acceptance transactions.
     */
    @Transactional
    public ProjectApplicationResponse reviewApplication(
            Long projectId,
            Long applicationId,
            Long currentUserId,
            ReviewApplicationRequest request
    ) {
        // Pessimistic lock on project to serialize concurrent reviews and ensure safe capacity enforcement
        Project project = projectRepository.findByIdWithPessimisticLock(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (!project.getOwner().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Only the project owner can review project applications");
        }

        ProjectApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        if (!application.getProject().getId().equals(projectId)) {
            throw new BadRequestException("Application does not belong to the specified project");
        }

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BadRequestException("Application has already been reviewed (current status: " + application.getStatus() + ")");
        }

        User reviewer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        if (request.status() == ApplicationStatus.ACCEPTED) {
            Team team = project.getTeam();
            if (team == null) {
                throw new BadRequestException("Project does not have an active team");
            }

            // Safe concurrency check on team capacity
            long currentTeamSize = teamMemberRepository.countByIdTeamId(team.getId());
            if (currentTeamSize >= project.getMaxTeamSize()) {
                throw new BadRequestException("Cannot accept application: Team has already reached maximum capacity of " + project.getMaxTeamSize());
            }

            // Add applicant to team members
            TeamMember newMember = new TeamMember(team, application.getApplicant(), TeamRole.MEMBER);
            teamMemberRepository.save(newMember);

            // Auto-transition to IN_PROGRESS if team is now full
            if (currentTeamSize + 1 >= project.getMaxTeamSize()) {
                project.setStatus(ProjectStatus.IN_PROGRESS);
                projectRepository.save(project);
            }

            application.setStatus(ApplicationStatus.ACCEPTED);

            // Trigger notification to applicant
            notificationService.createNotification(
                    application.getApplicant(),
                    reviewer,
                    NotificationType.APPLICATION_ACCEPTED,
                    "Application Accepted!",
                    "Congratulations! Your application to join '" + project.getTitle() + "' was accepted. You are now on the team.",
                    project.getId()
            );
        } else if (request.status() == ApplicationStatus.REJECTED) {
            application.setStatus(ApplicationStatus.REJECTED);
            application.setRejectionReason(request.rejectionReason());

            // Trigger notification to applicant
            notificationService.createNotification(
                    application.getApplicant(),
                    reviewer,
                    NotificationType.APPLICATION_REJECTED,
                    "Application Decision",
                    "Your application to join '" + project.getTitle() + "' was not accepted.",
                    project.getId()
            );
        } else {
            throw new BadRequestException("Invalid review decision status. Must be ACCEPTED or REJECTED");
        }

        application.setReviewedBy(reviewer);
        application.setReviewedAt(Instant.now());
        ProjectApplication saved = applicationRepository.save(application);

        List<UserSkillResponse> applicantSkills = userSkillRepository.findByUserIdWithSkill(saved.getApplicant().getId())
                .stream()
                .map(UserSkillResponse::fromEntity)
                .toList();

        return ProjectApplicationResponse.fromEntity(saved, applicantSkills);
    }

    @Transactional
    public void withdrawApplication(Long projectId, Long applicantId) {
        ProjectApplication application = applicationRepository.findByProjectIdAndApplicantId(projectId, applicantId)
                .orElseThrow(() -> new ResourceNotFoundException("No application found for project " + projectId));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BadRequestException("Cannot withdraw application with status: " + application.getStatus() + ". Only PENDING applications may be withdrawn.");
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        applicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public List<ProjectApplicationResponse> getProjectApplications(Long projectId, Long currentUserId, ApplicationStatus status) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (!project.getOwner().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Only the project owner can view applications for this project");
        }

        List<ProjectApplication> applications;
        if (status != null) {
            applications = applicationRepository.findByProjectIdAndStatusWithDetails(projectId, status);
        } else {
            applications = applicationRepository.findByProjectIdWithDetails(projectId);
        }

        return applications.stream()
                .map(app -> {
                    List<UserSkillResponse> skills = userSkillRepository.findByUserIdWithSkill(app.getApplicant().getId())
                            .stream()
                            .map(UserSkillResponse::fromEntity)
                            .toList();
                    return ProjectApplicationResponse.fromEntity(app, skills);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectApplicationResponse> getMyApplications(Long currentUserId) {
        return applicationRepository.findByApplicantIdWithProject(currentUserId)
                .stream()
                .map(app -> {
                    List<UserSkillResponse> skills = userSkillRepository.findByUserIdWithSkill(currentUserId)
                            .stream()
                            .map(UserSkillResponse::fromEntity)
                            .toList();
                    return ProjectApplicationResponse.fromEntity(app, skills);
                })
                .toList();
    }
}
