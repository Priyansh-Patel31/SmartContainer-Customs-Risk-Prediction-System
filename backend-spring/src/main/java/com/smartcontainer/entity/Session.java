package com.smartcontainer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Session entity — tracks active login sessions for users.
 */
@Entity
@Table(name = "sessions", indexes = {
        @Index(name = "idx_session_user", columnList = "userId"),
        @Index(name = "idx_session_login", columnList = "loginTime")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private String device;
    private String ip;
    private String userAgent;

    @Builder.Default
    private LocalDateTime loginTime = LocalDateTime.now();

    private LocalDateTime lastSeen;
}
