package com.smartcontainer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Message entity — individual chat message in a conversation.
 */
@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_msg_msgid", columnList = "messageId", unique = true),
        @Index(name = "idx_msg_conv", columnList = "conversationId"),
        @Index(name = "idx_msg_sender", columnList = "senderId"),
        @Index(name = "idx_msg_conv_time", columnList = "conversationId, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String messageId;

    @Column(nullable = false)
    private Long conversationId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private String senderRole;  // admin, officer, viewer, system

    @Column(columnDefinition = "TEXT")
    private String messageText;

    private String attachmentUrl;
    private String attachmentName;
    private String attachmentMime;

    @Column(columnDefinition = "TEXT")
    private String readByJson;  // JSON array of user IDs

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
