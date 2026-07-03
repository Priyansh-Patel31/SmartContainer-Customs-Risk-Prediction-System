package com.smartcontainer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * AuditLog entity — immutable record of user actions for compliance.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_timestamp_action", columnList = "timestamp, action"),
        @Index(name = "idx_audit_user", columnList = "userId, timestamp"),
        @Index(name = "idx_audit_entity", columnList = "entityType, entityId"),
        @Index(name = "idx_audit_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String username;
    private String role;

    @Column(nullable = false)
    private String action;  // LOGIN, LOGOUT, UPLOAD_DATASET, etc.

    private String entityType;  // Container, Job, User, Report, ShipmentTrack
    private String entityId;

    private String ip;
    private String userAgent;
    private String requestId;

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
