package com.smartcontainer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Conversation entity — container-linked chat thread between exporter and admin/officer.
 */
@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conv_convid", columnList = "conversationId", unique = true),
        @Index(name = "idx_conv_container", columnList = "containerId"),
        @Index(name = "idx_conv_exporter", columnList = "exporterId"),
        @Index(name = "idx_conv_admin", columnList = "adminId")
},
        uniqueConstraints = @UniqueConstraint(name = "uk_container_exporter", columnNames = {"containerId", "exporterId"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String conversationId;

    @Column(nullable = false)
    private String containerId;

    @Column(nullable = false)
    private String exporterId;

    private Long adminId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
