package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatId(String chatId);
    List<ChatMessage> findByChatIdContainingIgnoreCase(String username);
    @Query("SELECT m FROM ChatMessage m WHERE LOWER(m.chatId) LIKE LOWER(CONCAT(:username, '_%')) OR LOWER(m.chatId) LIKE LOWER(CONCAT('%_', :username))")
    List<ChatMessage> findByUserInChatId(@Param("username") String username);

}