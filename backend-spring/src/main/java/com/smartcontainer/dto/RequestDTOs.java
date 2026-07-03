package com.smartcontainer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * All request DTOs — used for validation with Jakarta annotations.
 */
public class RequestDTOs {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;
        @NotBlank(message = "Password is required")
        private String password;
        private String device;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        private String full_name;
        private String role;
        private String phone_number;
        private String department;
        private String profile_photo;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class UpdateProfileRequest {
        private String full_name;
        @Email(message = "Invalid email format")
        private String email;
        private String phone_number;
        private String department;
        private String profile_photo;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String current_password;
        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String new_password;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class AssignContainerRequest {
        @NotBlank(message = "assigned_to is required")
        private String assigned_to;
        private String notes;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class UpdateStatusRequest {
        @NotBlank(message = "inspection_status is required")
        private String inspection_status;
        private String notes;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class AddNoteRequest {
        @NotBlank(message = "text is required")
        private String text;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class LinkVesselRequest {
        @NotBlank(message = "container_id is required")
        private String container_id;
        private String vessel_imo;
        private String vessel_name;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class StartChatRequest {
        @NotBlank(message = "container_id is required")
        private String container_id;
        @NotBlank(message = "exporter_id is required")
        private String exporter_id;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class SendMessageRequest {
        @NotBlank(message = "conversation_id is required")
        private String conversation_id;
        private String message_text;
        private String attachment_url;
        private String attachment_name;
        private String attachment_mime;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class NotificationSettingsRequest {
        private Boolean highRisk;
        private Boolean anomaly;
        private Boolean weeklySummary;
    }
}
