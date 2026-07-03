package com.smartcontainer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcontainer.entity.Container;
import com.smartcontainer.entity.User;
import com.smartcontainer.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.smartcontainer.repository.ContainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WorkflowService — manages the customs inspection queue and actions.
 */
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final ContainerRepository containerRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Page<Container> getQueue(int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), limit);
        return containerRepository.getInspectionQueue(pageable);
    }

    public Container getContainer(String id) {
        return containerRepository.findByContainerId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Container not found"));
    }

    public Container assignContainer(String containerId, String assignedTo, String notes, User user) {
        Container container = getContainer(containerId);
        container.setAssignedTo(assignedTo);
        if ("NEW".equals(container.getInspectionStatus())) {
            container.setInspectionStatus("ASSIGNED");
        }
        
        if (notes != null && !notes.trim().isEmpty()) {
            addNoteToContainer(container, notes, user);
        } else {
            containerRepository.save(container);
        }

        auditService.log(user, "ASSIGN_CONTAINER", "Container", containerId);
        return container;
    }

    public Container updateStatus(String containerId, String status, String notes, User user) {
        Container container = getContainer(containerId);
        container.setInspectionStatus(status);
        
        if (notes != null && !notes.trim().isEmpty()) {
            addNoteToContainer(container, notes, user);
        } else {
            containerRepository.save(container);
        }

        auditService.log(user, "UPDATE_STATUS", "Container", containerId);
        return container;
    }

    public Container addNote(String containerId, String text, User user) {
        Container container = getContainer(containerId);
        addNoteToContainer(container, text, user);
        auditService.log(user, "ADD_NOTE", "Container", containerId);
        return container;
    }

    private void addNoteToContainer(Container container, String text, User user) {
        List<Map<String, Object>> notes = new ArrayList<>();
        try {
            if (container.getNotesJson() != null && !container.getNotesJson().isEmpty()) {
                notes = objectMapper.readValue(container.getNotesJson(), new TypeReference<List<Map<String, Object>>>() {});
            }
            
            Map<String, Object> note = new HashMap<>();
            note.put("text", text);
            note.put("author", user.getUsername());
            note.put("author_id", user.getId());
            note.put("timestamp", java.time.LocalDateTime.now().toString());
            notes.add(note);
            
            container.setNotesJson(objectMapper.writeValueAsString(notes));
            containerRepository.save(container);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to process notes JSON", e);
        }
    }

    public List<Container> getNotifications(int limit) {
        return containerRepository.getRecentInspectionActivity(PageRequest.of(0, limit));
    }
}
