package com.smartcontainer.service;

import com.smartcontainer.dto.RequestDTOs;
import com.smartcontainer.entity.Session;
import com.smartcontainer.entity.User;
import com.smartcontainer.entity.NotificationSettings;
import com.smartcontainer.exception.GlobalExceptionHandler.ConflictException;
import com.smartcontainer.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.smartcontainer.exception.GlobalExceptionHandler.UnauthorizedException;
import com.smartcontainer.repository.SessionRepository;
import com.smartcontainer.repository.UserRepository;
import com.smartcontainer.repository.NotificationSettingsRepository;
import com.smartcontainer.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * AuthService — handles login, registration, and profile management.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

    public Map<String, Object> login(RequestDTOs.LoginRequest request, String ip, String userAgent) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", "Invalid username or password"));

        if (!user.getIsActive()) {
            throw new UnauthorizedException("USER_INACTIVE", "Account is inactive");
        }

        if (!user.checkPassword(request.getPassword())) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid username or password");
        }

        // Create session
        Session session = Session.builder()
                .userId(user.getId())
                .device(request.getDevice() != null ? request.getDevice() : "Unknown")
                .ip(ip)
                .userAgent(userAgent)
                .lastSeen(LocalDateTime.now())
                .build();
        sessionRepository.save(session);

        // Generate token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getRole(), session.getId());

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        auditService.log(user, "LOGIN", "User", String.valueOf(user.getId()), ip, userAgent);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("expiresIn", jwtTokenProvider.getExpiryString());
        
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("role", user.getRole());
        userMap.put("full_name", user.getFullName());
        userMap.put("email", user.getEmail());
        userMap.put("profile_photo", user.getProfilePhoto());
        response.put("user", userMap);

        return response;
    }

    public User register(RequestDTOs.RegisterRequest request, User adminUser) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("USERNAME_EXISTS", "Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("EMAIL_EXISTS", "Email already registered");
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .role(request.getRole() != null ? request.getRole() : "exporter")
                .fullName(request.getFull_name())
                .phoneNumber(request.getPhone_number())
                .department(request.getDepartment())
                .profilePhoto(request.getProfile_photo())
                .createdBy(adminUser.getId())
                .isActive(true)
                .build();
        
        newUser.setRawPassword(request.getPassword());
        userRepository.save(newUser);

        // Create default notification settings
        NotificationSettings notifSettings = NotificationSettings.builder()
                .userId(newUser.getId())
                .build();
        notificationSettingsRepository.save(notifSettings);

        auditService.log(adminUser, "CREATE_USER", "User", String.valueOf(newUser.getId()));
        
        return newUser;
    }

    public User updateProfile(User user, RequestDTOs.UpdateProfileRequest request) {
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("EMAIL_EXISTS", "Email already in use");
            }
            user.setEmail(request.getEmail());
        }
        
        if (request.getFull_name() != null) user.setFullName(request.getFull_name());
        if (request.getPhone_number() != null) user.setPhoneNumber(request.getPhone_number());
        if (request.getDepartment() != null) user.setDepartment(request.getDepartment());
        if (request.getProfile_photo() != null) user.setProfilePhoto(request.getProfile_photo());

        userRepository.save(user);
        auditService.log(user, "UPDATE_PROFILE");
        return user;
    }

    public void changePassword(User user, RequestDTOs.ChangePasswordRequest request) {
        if (!user.checkPassword(request.getCurrent_password())) {
            throw new UnauthorizedException("INVALID_PASSWORD", "Current password is incorrect");
        }
        
        user.setRawPassword(request.getNew_password());
        userRepository.save(user);
        auditService.log(user, "CHANGE_PASSWORD");
    }

    public void logout(User user, Long sessionId, String ip, String userAgent) {
        if (sessionId != null) {
            sessionRepository.deleteById(sessionId);
        }
        auditService.log(user, "LOGOUT", "User", String.valueOf(user.getId()), ip, userAgent);
    }
}
