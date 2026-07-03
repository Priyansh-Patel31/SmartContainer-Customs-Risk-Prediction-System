package com.smartcontainer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcontainer.dto.RequestDTOs;
import com.smartcontainer.entity.Conversation;
import com.smartcontainer.entity.Message;
import com.smartcontainer.entity.User;
import com.smartcontainer.exception.GlobalExceptionHandler.ConflictException;
import com.smartcontainer.repository.ConversationRepository;
import com.smartcontainer.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ChatService — handles conversations and messaging between exporters and officers.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Conversation startConversation(String containerId, String exporterId, User adminUser) {
        if (conversationRepository.findByContainerIdAndExporterId(containerId, exporterId).isPresent()) {
            throw new ConflictException("CONV_EXISTS", "A conversation already exists for this container and exporter.");
        }

        Conversation conv = Conversation.builder()
                .conversationId(UUID.randomUUID().toString())
                .containerId(containerId)
                .exporterId(exporterId)
                .adminId(adminUser.getId())
                .build();
        
        return conversationRepository.save(conv);
    }

    public Page<Conversation> getConversations(User user, int page, int limit) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), limit, Sort.by(Sort.Direction.DESC, "updatedAt"));
        if ("exporter".equals(user.getRole())) {
            return conversationRepository.findByExporterId(String.valueOf(user.getId()), pageRequest);
        } else {
            return conversationRepository.findAll(pageRequest);
        }
    }

    public List<Message> getMessages(String conversationId, int limit) {
        Conversation conv = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        return messageRepository.findByConversationIdOrderByTimestampDesc(conv.getId(), PageRequest.of(0, limit));
    }

    public Message sendMessage(RequestDTOs.SendMessageRequest request, User sender) {
        Conversation conv = conversationRepository.findByConversationId(request.getConversation_id())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        List<Long> readBy = new ArrayList<>();
        readBy.add(sender.getId());
        String readByJson = "[]";
        try {
            readByJson = objectMapper.writeValueAsString(readBy);
        } catch (JsonProcessingException ignored) {}

        Message message = Message.builder()
                .messageId(UUID.randomUUID().toString())
                .conversationId(conv.getId())
                .senderId(sender.getId())
                .senderRole(sender.getRole())
                .messageText(request.getMessage_text())
                .attachmentUrl(request.getAttachment_url())
                .attachmentName(request.getAttachment_name())
                .attachmentMime(request.getAttachment_mime())
                .readByJson(readByJson)
                .build();
                
        messageRepository.save(message);

        // Update conversation timestamp
        conv.setUpdatedAt(java.time.LocalDateTime.now());
        conversationRepository.save(conv);

        return message;
    }
}
