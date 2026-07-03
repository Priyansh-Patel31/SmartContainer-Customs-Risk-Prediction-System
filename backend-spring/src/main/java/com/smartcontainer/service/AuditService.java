package com.smartcontainer.service;

import com.smartcontainer.entity.AuditLog;
import com.smartcontainer.entity.User;
import com.smartcontainer.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AuditService — records user actions for compliance.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(User user, String action, String entityType, String entityId, String ip, String userAgent) {
        AuditLog log = AuditLog.builder()
                .userId(user != null ? user.getId() : null)
                .username(user != null ? user.getUsername() : "system")
                .role(user != null ? user.getRole() : "system")
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .ip(ip)
                .userAgent(userAgent)
                .build();
        auditLogRepository.save(log);
    }

    public void log(User user, String action) {
        log(user, action, null, null, null, null);
    }

    public void log(User user, String action, String entityType, String entityId) {
        log(user, action, entityType, entityId, null, null);
    }
}
