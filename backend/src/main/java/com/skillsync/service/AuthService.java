package com.skillsync.service;

import com.skillsync.dto.auth.AuthResponse;
import com.skillsync.dto.auth.LoginRequest;
import com.skillsync.dto.auth.RegisterRequest;
import com.skillsync.dto.user.UserProfileResponse;
import com.skillsync.dto.user.UserSkillResponse;
import com.skillsync.entity.User;
import com.skillsync.enums.Role;
import com.skillsync.exception.DuplicateResourceException;
import com.skillsync.exception.ResourceNotFoundException;
import com.skillsync.repository.UserRepository;
import com.skillsync.repository.UserSkillRepository;
import com.skillsync.security.JwtTokenProvider;
import com.skillsync.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       UserSkillRepository userSkillRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.userSkillRepository = userSkillRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User with email '" + request.email() + "' already exists");
        }

        User user = new User(
                request.email().toLowerCase().trim(),
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                request.department(),
                request.yearOfStudy()
        );
        user.setRole(Role.ROLE_STUDENT);

        User savedUser = userRepository.save(user);

        String token = tokenProvider.generateTokenFromUser(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        return AuthResponse.of(token, savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), savedUser.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase().trim(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        String token = tokenProvider.generateToken(authentication);

        return AuthResponse.of(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        List<UserSkillResponse> skills = userSkillRepository.findByUserIdWithSkill(user.getId())
                .stream()
                .map(UserSkillResponse::fromEntity)
                .toList();

        return UserProfileResponse.fromEntity(user, skills);
    }
}
