package com.smartcontainer.service;

import com.smartcontainer.entity.NotificationSettings;
import com.smartcontainer.entity.Session;
import com.smartcontainer.entity.User;
import com.smartcontainer.entity.AuditLog;
import com.smartcontainer.repository.NotificationSettingsRepository;
import com.smartcontainer.repository.SessionRepository;
import com.smartcontainer.repository.UserRepository;
import com.smartcontainer.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * UserService — handles extended profile, sessions, logs, and system access.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;

    public Map<String, Object> getExtendedProfile(User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("role", user.getRole());
        profile.put("full_name", user.getFullName());
        profile.put("email", user.getEmail());
        profile.put("phone_number", user.getPhoneNumber());
        profile.put("department", user.getDepartment());
        profile.put("profile_photo", user.getProfilePhoto());
        profile.put("last_login", user.getLastLogin());
        profile.put("created_at", user.getCreatedAt());

        Optional<NotificationSettings> settings = notificationSettingsRepository.findByUserId(user.getId());
        settings.ifPresent(s -> {
            Map<String, Object> notifMap = new HashMap<>();
            notifMap.put("highRisk", s.getHighRisk());
            notifMap.put("anomaly", s.getAnomaly());
            notifMap.put("weeklySummary", s.getWeeklySummary());
            profile.put("notification_settings", notifMap);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("profile", profile);
        return response;
    }

    @Transactional
    public User updateProfile(User user, Map<String, Object> updates) {
        if (updates.containsKey("full_name")) user.setFullName((String) updates.get("full_name"));
        if (updates.containsKey("email")) user.setEmail((String) updates.get("email"));
        if (updates.containsKey("phone_number")) user.setPhoneNumber((String) updates.get("phone_number"));
        if (updates.containsKey("department")) user.setDepartment((String) updates.get("department"));
        if (updates.containsKey("profile_photo")) user.setProfilePhoto((String) updates.get("profile_photo"));
        return userRepository.save(user);
    }

    public List<Session> getActiveSessions(User user) {
        return sessionRepository.findByUserId(user.getId());
    }

    @Transactional
    public void logoutAll(User user, String ip, String userAgent) {
        sessionRepository.deleteByUserId(user.getId());
        auditService.log(user, "LOGOUT_ALL", "User", String.valueOf(user.getId()), ip, userAgent);
    }

    public List<AuditLog> getActivityLogs(User user, int limit) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(user.getId(), PageRequest.of(0, limit));
    }

    public NotificationSettings getNotificationSettings(User user) {
        return notificationSettingsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    NotificationSettings ns = NotificationSettings.builder().userId(user.getId()).build();
                    return notificationSettingsRepository.save(ns);
                });
    }

    public NotificationSettings updateNotificationSettings(User user, Boolean highRisk, Boolean anomaly, Boolean weeklySummary) {
        NotificationSettings settings = getNotificationSettings(user);
        
        if (highRisk != null) settings.setHighRisk(highRisk);
        if (anomaly != null) settings.setAnomaly(anomaly);
        if (weeklySummary != null) settings.setWeeklySummary(weeklySummary);
        
        return notificationSettingsRepository.save(settings);
    }

    public Map<String, Object> getSystemAccess(User user) {
        Map<String, Object> access = new HashMap<>();
        access.put("role", user.getRole());
        access.put("can_approve", "admin".equals(user.getRole()) || "officer".equals(user.getRole()));
        access.put("can_override", "admin".equals(user.getRole()));
        access.put("can_manage_users", "admin".equals(user.getRole()));
        access.put("can_upload", "admin".equals(user.getRole()) || "officer".equals(user.getRole()));
        return access;
    }
}
