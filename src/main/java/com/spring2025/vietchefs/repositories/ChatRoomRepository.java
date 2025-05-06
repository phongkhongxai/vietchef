package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findBySenderIdAndRecipientId(String senderId, String recipientId);
    Optional<ChatRoom> findByChatId(String chatId);

}