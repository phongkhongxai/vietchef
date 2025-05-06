package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.ChatRoom;
import com.spring2025.vietchefs.repositories.ChatRoomRepository;
import com.spring2025.vietchefs.services.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    @Override
    public Optional<String> getChatRoomId(String senderId, String recipientId, boolean createNewRoomIfNotExists) {
        String chatId = senderId.compareTo(recipientId) < 0
                ? String.format("%s_%s", senderId, recipientId)
                : String.format("%s_%s", recipientId, senderId);

        Optional<ChatRoom> chatRoom = chatRoomRepository.findByChatId(chatId);

        if (chatRoom.isPresent()) {
            return Optional.of(chatId);
        } else if (createNewRoomIfNotExists) {
            ChatRoom newRoom = ChatRoom.builder()
                    .chatId(chatId)
                    .senderId(senderId)
                    .recipientId(recipientId)
                    .build();
            chatRoomRepository.save(newRoom);
            return Optional.of(chatId);
        }

        return Optional.empty();
    }

    @Override
    public String createChatId(String senderId, String recipientId) {
        String chatId = senderId.compareTo(recipientId) < 0
                ? String.format("%s_%s", senderId, recipientId)
                : String.format("%s_%s", recipientId, senderId);

        ChatRoom chatRoom = ChatRoom
                .builder()
                .chatId(chatId)
                .senderId(senderId)
                .recipientId(recipientId)
                .build();

        chatRoomRepository.save(chatRoom);

        return chatId;
    }
}