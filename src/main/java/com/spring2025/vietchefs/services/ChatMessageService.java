package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.ChatMessage;
import com.spring2025.vietchefs.models.payload.dto.ChatMessageDto;

import java.util.List;

public interface ChatMessageService {
    void processMessage(ChatMessageDto chatMessage);
    List<ChatMessageDto> findChatMessages(String senderId, String recipientId);
    List<ChatMessageDto> getConversationsOfUser(String username);
}