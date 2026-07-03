package com.smartcontainer.repository;

import com.smartcontainer.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByConversationId(String conversationId);
    Optional<Conversation> findByContainerIdAndExporterId(String containerId, String exporterId);
    Page<Conversation> findByExporterId(String exporterId, Pageable pageable);
    Page<Conversation> findByAdminId(Long adminId, Pageable pageable);
}
