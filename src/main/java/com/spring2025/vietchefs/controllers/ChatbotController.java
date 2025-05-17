package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.responseModel.ChatboxResponse;
import com.spring2025.vietchefs.services.impl.ChatbotService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chatbox")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @Autowired
    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_CHEF')")
    @PostMapping("/ask")
    public ResponseEntity<ChatboxResponse> chatWithAI(@RequestParam(value = "message", defaultValue = "Sơn Tùng sinh ngày tháng năm nào?") String message) {
        return ResponseEntity.ok(chatbotService.processMessage(message));
    }
}
