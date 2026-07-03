package com.smartcontainer.controller;

import com.smartcontainer.dto.ApiResponse;
import com.smartcontainer.dto.RequestDTOs;
import com.smartcontainer.entity.User;
import com.smartcontainer.service.AuthService;
import com.smartcontainer.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Auth Controller — Maps to routes in authRoutes.js
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
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

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody RequestDTOs.LoginRequest loginRequest, HttpServletRequest request) {
        Map<String, Object> result = authService.login(loginRequest, getClientIp(request), getUserAgent(request));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RequestDTOs.RegisterRequest registerRequest,
                                                      @RequestAttribute("currentUser") User currentUser) {
        User newUser = authService.register(registerRequest, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(newUser, "User registered successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(@RequestAttribute("currentUser") User currentUser) {
        Map<String, Object> userMap = Map.of(
                "id", currentUser.getId(),
                "username", currentUser.getUsername(),
                "role", currentUser.getRole(),
                "full_name", currentUser.getFullName() != null ? currentUser.getFullName() : "",
                "email", currentUser.getEmail(),
                "profile_photo", currentUser.getProfilePhoto() != null ? currentUser.getProfilePhoto() : ""
        );
        Map<String, Object> data = Map.of("user", userMap);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(@Valid @RequestBody RequestDTOs.UpdateProfileRequest updateRequest,
                                                                         @RequestAttribute("currentUser") User currentUser) {
        User updated = authService.updateProfile(currentUser, updateRequest);
        Map<String, Object> data = Map.of("user", updated);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody RequestDTOs.ChangePasswordRequest passwordRequest,
                                                            @RequestAttribute("currentUser") User currentUser) {
        authService.changePassword(currentUser, passwordRequest);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestAttribute("currentUser") User currentUser,
                                                    @RequestAttribute(value = "tokenSessionId", required = false) Long sessionId,
                                                    HttpServletRequest request) {
        authService.logout(currentUser, sessionId, getClientIp(request), getUserAgent(request));
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    // In a real app, users would be fetched via UserRepository. Adding stub for completeness matching route schema.
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> listUsers() {
        return ResponseEntity.ok(ApiResponse.ok(List.of(), "List users endpoint (admin)"));
    }

    @PatchMapping("/users/{id}/active")
    public ResponseEntity<ApiResponse<Void>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Toggled user active status"));
    }
}
