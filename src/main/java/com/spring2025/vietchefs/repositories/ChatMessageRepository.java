package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatId(String chatId);
    List<ChatMessage> findByChatIdContainingIgnoreCase(String username);
}