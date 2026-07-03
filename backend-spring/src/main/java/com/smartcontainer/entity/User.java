package com.smartcontainer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

/**
 * User entity — supports roles: admin, officer, viewer, exporter.
 * BCrypt password hashing handled in service layer.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username", unique = true),
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_role", columnList = "role"),
        @Index(name = "idx_user_active", columnList = "isActive")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Builder.Default
    private String role = "exporter";  // admin, officer, viewer, exporter

    private String fullName;
    private String phoneNumber;
    private String department;
    private String profilePhoto;

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime lastLogin;

    private Long createdBy;  // ID of admin who created this user

    // Embedded notification settings
    @Builder.Default
    private Boolean notifHighRisk = true;
    @Builder.Default
    private Boolean notifAnomaly = false;
    @Builder.Default
    private Boolean notifWeeklySummary = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public void setRawPassword(String plaintext) {
        this.passwordHash = encoder.encode(plaintext);
    }

    public boolean checkPassword(String plaintext) {
        return encoder.matches(plaintext, this.passwordHash);
    }
}
