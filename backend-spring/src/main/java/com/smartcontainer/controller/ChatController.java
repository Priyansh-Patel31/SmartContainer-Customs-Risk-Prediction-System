package com.smartcontainer.controller;

import com.smartcontainer.dto.ApiResponse;
import com.smartcontainer.dto.RequestDTOs;
import com.smartcontainer.entity.Conversation;
import com.smartcontainer.entity.Message;
import com.smartcontainer.entity.User;
import com.smartcontainer.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Chat Controller — Maps to routes in chatRoutes.js
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<Conversation>> startConversation(@Valid @RequestBody RequestDTOs.StartChatRequest request,
                                                                       @RequestAttribute("currentUser") User currentUser) {
        Conversation conv = chatService.startConversation(request.getContainer_id(), request.getExporter_id(), currentUser);
        return ResponseEntity.ok(ApiResponse.ok(conv, "Conversation started"));
    }

    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getConversations(@RequestAttribute("currentUser") User currentUser,
                                                                @RequestParam(defaultValue = "1") int page,
                                                                @RequestParam(defaultValue = "20") int limit) {
        Page<Conversation> pagedResult = chatService.getConversations(currentUser, page, limit);
        Map<String, Object> response = Map.of(
                "success", true,
                "data", pagedResult.getContent(),
                "total", pagedResult.getTotalElements(),
                "page", page,
                "limit", limit
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<Message>>> getMessages(@PathVariable String conversationId,
                                                                  @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getMessages(conversationId, limit)));
    }

    @PostMapping("/message")
    public ResponseEntity<ApiResponse<Message>> sendMessage(@Valid @RequestBody RequestDTOs.SendMessageRequest request,
                                                            @RequestAttribute("currentUser") User currentUser) {
        Message message = chatService.sendMessage(request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(message));
    }
}
