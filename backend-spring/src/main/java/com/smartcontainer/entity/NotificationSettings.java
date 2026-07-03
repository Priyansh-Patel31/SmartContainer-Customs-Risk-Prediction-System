package com.smartcontainer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * NotificationSettings entity — per-user notification preferences.
 */
@Entity
@Table(name = "notification_settings", indexes = {
        @Index(name = "idx_notif_user", columnList = "userId", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Builder.Default
    private Boolean highRisk = true;

    @Builder.Default
    private Boolean anomaly = false;

    @Builder.Default
    private Boolean weeklySummary = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
