package com.skillsync.service;

import com.skillsync.dto.user.UpdateProfileRequest;
import com.skillsync.dto.user.UserProfileResponse;
import com.skillsync.dto.user.UserSkillRequest;
import com.skillsync.dto.user.UserSkillResponse;
import com.skillsync.entity.Skill;
import com.skillsync.entity.User;
import com.skillsync.entity.UserSkill;
import com.skillsync.exception.ResourceNotFoundException;
import com.skillsync.repository.SkillRepository;
import com.skillsync.repository.UserRepository;
import com.skillsync.repository.UserSkillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;

    public UserService(UserRepository userRepository,
                       SkillRepository skillRepository,
                       UserSkillRepository userSkillRepository) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<UserSkillResponse> skills = userSkillRepository.findByUserIdWithSkill(userId)
                .stream()
                .map(UserSkillResponse::fromEntity)
                .toList();

        return UserProfileResponse.fromEntity(user, skills);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setFullName(request.fullName().trim());
        user.setHeadline(request.headline());
        user.setBio(request.bio());
        user.setDepartment(request.department());
        user.setYearOfStudy(request.yearOfStudy());
        user.setGithubUrl(request.githubUrl());
        user.setLinkedinUrl(request.linkedinUrl());

        User updatedUser = userRepository.save(user);

        List<UserSkillResponse> skills = userSkillRepository.findByUserIdWithSkill(userId)
                .stream()
                .map(UserSkillResponse::fromEntity)
                .toList();

        return UserProfileResponse.fromEntity(updatedUser, skills);
    }

    @Transactional
    public List<UserSkillResponse> updateUserSkills(Long userId, List<UserSkillRequest> requests) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Clear existing skills
        userSkillRepository.deleteByIdUserId(userId);

        List<UserSkill> toSave = new ArrayList<>();
        for (UserSkillRequest req : requests) {
            Skill skill = skillRepository.findById(req.skillId())
                    .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", req.skillId()));

            UserSkill userSkill = new UserSkill(user, skill, req.proficiency(), req.yearsOfExperience());
            toSave.add(userSkill);
        }

        List<UserSkill> saved = userSkillRepository.saveAll(toSave);
        return saved.stream()
                .map(UserSkillResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<UserProfileResponse> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable)
                .map(user -> {
                    List<UserSkillResponse> skills = userSkillRepository.findByUserIdWithSkill(user.getId())
                            .stream()
                            .map(UserSkillResponse::fromEntity)
                            .toList();
                    return UserProfileResponse.fromEntity(user, skills);
                });
    }
}
