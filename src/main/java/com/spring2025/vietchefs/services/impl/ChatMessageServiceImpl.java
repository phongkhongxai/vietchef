package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.ChatMessage;
import com.spring2025.vietchefs.models.entity.Notification;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.ChatMessageDto;
import com.spring2025.vietchefs.models.payload.requestModel.NotificationRequest;
import com.spring2025.vietchefs.repositories.ChatMessageRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.ChatMessageService;
import com.spring2025.vietchefs.services.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private static final String QUEUE_MESSAGES = "/queue/messages";

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public void processMessage(ChatMessageDto chatMessage) {
        validateMessage(chatMessage);
        ChatMessage msg = modelMapper.map(chatMessage,ChatMessage.class);
//        ZoneId hoChiMinhZone = ZoneId.of("Asia/Ho_Chi_Minh");
//        ZonedDateTime zonedTime = chatMessage.getTimestamp().atZone(hoChiMinhZone);
//        OffsetDateTime utcTime = zonedTime.withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
//        msg.setTimestamp(utcTime.toLocalDateTime());
        User user = userRepository.findByUsername(chatMessage.getRecipientId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found with username."));
        msg = saveMessage(msg);
        NotificationRequest chefNotification = NotificationRequest.builder()
                .userId(user.getId())
                .title("New messages")
                .body("New messages from "+msg.getSenderName())
                .screen("ChatScreen")
                .notiType("CHAT_NOTIFY")
                .build();
        notificationService.sendPushNotification(chefNotification);
        messagingTemplate.convertAndSendToUser(msg.getRecipientId(), QUEUE_MESSAGES, msg);
    }

    public ChatMessage saveMessage(ChatMessage chatMessage) {
        String chatId = chatRoomService
                .getChatRoomId(chatMessage.getSenderId(), chatMessage.getRecipientId(), true)
                .orElseThrow();
        chatMessage.setChatId(chatId);
        chatMessageRepository.save(chatMessage);
        return chatMessage;
    }

    @Override
    public List<ChatMessageDto> findChatMessages(String senderId, String recipientId) {
        String chatId = chatRoomService
                .getChatRoomId(senderId, recipientId, false)
                .orElseThrow();
        List<ChatMessage> messages = chatMessageRepository.findByChatId(chatId);

        return messages.stream()
                .map(msg -> modelMapper.map(msg, ChatMessageDto.class))
                .toList();
    }

    @Override
    public List<ChatMessageDto> getConversationsOfUser(String username) {
        List<ChatMessage> chatMessages = chatMessageRepository.findByChatIdContainingIgnoreCase(username);
        return chatMessages.stream()
                .collect(Collectors.groupingBy(
                        ChatMessage::getChatId,
                        Collectors.maxBy(Comparator.comparing(ChatMessage::getTimestamp))
                ))
                .values()
                .stream()
                .flatMap(Optional::stream)
                .map(msg -> {
                    ChatMessageDto dto = modelMapper.map(msg, ChatMessageDto.class);
                    if (msg.getSenderId().equals(username)) {
                        dto.setSenderName("You");
                    }
                    return dto;
                })
                .toList();
    }

    private void validateMessage(ChatMessageDto chatMessage) {
        if (chatMessage.getSenderId() == null || chatMessage.getSenderId().isEmpty()) {
            throw new VchefApiException(BAD_REQUEST,"error.chat.required.senderId");
        }
        if (chatMessage.getRecipientId() == null || chatMessage.getRecipientId().isEmpty()) {
            throw new VchefApiException(BAD_REQUEST,"error.chat.required.recipientId");
        }
        if (chatMessage.getContent() == null || chatMessage.getContent().isEmpty()) {
            throw new VchefApiException(BAD_REQUEST,"error.chat.required.content");
        }
        if (chatMessage.getContentType() == null || chatMessage.getContentType().isEmpty()) {
            throw new VchefApiException(BAD_REQUEST,"error.chat.required.contentType");
        }
    }
}