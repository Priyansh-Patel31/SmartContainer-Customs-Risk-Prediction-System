package com.smartcontainer.controller;

import com.smartcontainer.dto.ApiResponse;
import com.smartcontainer.dto.RequestDTOs;
import com.smartcontainer.entity.AuditLog;
import com.smartcontainer.entity.NotificationSettings;
import com.smartcontainer.entity.Session;
import com.smartcontainer.entity.User;
import com.smartcontainer.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * User Controller — Maps to routes in userRoutes.js
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@RequestAttribute("currentUser") User currentUser) {
        Map<String, Object> result = userService.getExtendedProfile(currentUser);
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/update-profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestAttribute("currentUser") User currentUser, @RequestBody Map<String, Object> updates) {
        User updatedUser = userService.updateProfile(currentUser, updates);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("user", updatedUser);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/active-sessions")
    public ResponseEntity<ApiResponse<List<Session>>> getSessions(@RequestAttribute("currentUser") User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getActiveSessions(currentUser)));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAllSessions(@RequestAttribute("currentUser") User currentUser,
                                                               HttpServletRequest request) {
        userService.logoutAll(currentUser, getClientIp(request), getUserAgent(request));
        return ResponseEntity.ok(ApiResponse.ok("Logged out from all devices"));
    }

    @GetMapping("/activity-logs")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getActivity(@RequestAttribute("currentUser") User currentUser,
                                                                   @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getActivityLogs(currentUser, limit)));
    }

    @GetMapping("/settings/notifications")
    public ResponseEntity<ApiResponse<NotificationSettings>> getNotificationSettings(@RequestAttribute("currentUser") User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getNotificationSettings(currentUser)));
    }

    @PutMapping("/settings/notifications")
    public ResponseEntity<ApiResponse<NotificationSettings>> updateNotificationSettings(
            @RequestAttribute("currentUser") User currentUser,
            @Valid @RequestBody RequestDTOs.NotificationSettingsRequest request) {
        NotificationSettings updated = userService.updateNotificationSettings(currentUser, request.getHighRisk(), request.getAnomaly(), request.getWeeklySummary());
        return ResponseEntity.ok(ApiResponse.ok(updated, "Notification settings updated"));
    }

    @GetMapping("/system-access")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemAccess(@RequestAttribute("currentUser") User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getSystemAccess(currentUser)));
    }
}
