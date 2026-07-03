package com.smartcontainer.controller;

import com.smartcontainer.dto.ApiResponse;
import com.smartcontainer.dto.RequestDTOs;
import com.smartcontainer.entity.Container;
import com.smartcontainer.entity.User;
import com.smartcontainer.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Workflow Controller — Maps to routes in queueRoutes.js and some containerRoutes.js
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping("/queue")
    public ResponseEntity<Map<String, Object>> getQueue(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int limit) {
        Page<Container> pagedResult = workflowService.getQueue(page, limit);
        Map<String, Object> response = Map.of(
                "success", true,
                "data", pagedResult.getContent(),
                "total", pagedResult.getTotalElements(),
                "page", page,
                "limit", limit
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/containers/{containerId}/assign")
    public ResponseEntity<ApiResponse<Container>> assignContainer(@PathVariable String containerId,
                                                                  @Valid @RequestBody RequestDTOs.AssignContainerRequest request,
                                                                  @RequestAttribute("currentUser") User currentUser) {
        Container updated = workflowService.assignContainer(containerId, request.getAssigned_to(), request.getNotes(), currentUser);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Container assigned successfully"));
    }

    @PatchMapping("/containers/{containerId}/status")
    public ResponseEntity<ApiResponse<Container>> updateStatus(@PathVariable String containerId,
                                                               @Valid @RequestBody RequestDTOs.UpdateStatusRequest request,
                                                               @RequestAttribute("currentUser") User currentUser) {
        Container updated = workflowService.updateStatus(containerId, request.getInspection_status(), request.getNotes(), currentUser);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Status updated successfully"));
    }

    @PostMapping("/containers/{containerId}/notes")
    public ResponseEntity<ApiResponse<Container>> addNote(@PathVariable String containerId,
                                                          @Valid @RequestBody RequestDTOs.AddNoteRequest request,
                                                          @RequestAttribute("currentUser") User currentUser) {
        Container updated = workflowService.addNote(containerId, request.getText(), currentUser);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Note added successfully"));
    }

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<Container>>> getNotifications(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(workflowService.getNotifications(limit)));
    }
}
