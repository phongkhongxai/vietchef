package com.spring2025.vietchefs.services.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.spring2025.vietchefs.models.entity.Dish;
import com.spring2025.vietchefs.models.entity.Notification;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.DishDto;
import com.spring2025.vietchefs.models.payload.dto.NotificationDto;
import com.spring2025.vietchefs.models.payload.requestModel.NotificationRequest;
import com.spring2025.vietchefs.models.payload.responseModel.DishesResponse;
import com.spring2025.vietchefs.models.payload.responseModel.NotificationsResponse;
import com.spring2025.vietchefs.repositories.NotificationRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    public NotificationsResponse getALlNotificationsOfUser(Long userId, int pageNo, int pageSize, String sortBy, String sortDir){
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "User not found.");
        }

        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(optionalUser.get().getId(),pageable);

        // get content for page object
        List<Notification> notificationList = notifications.getContent();

        List<NotificationDto> content = notificationList.stream().map(bt -> modelMapper.map(bt, NotificationDto.class)).collect(Collectors.toList());

        NotificationsResponse templatesResponse = new NotificationsResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(notifications.getNumber());
        templatesResponse.setPageSize(notifications.getSize());
        templatesResponse.setTotalElements(notifications.getTotalElements());
        templatesResponse.setTotalPages(notifications.getTotalPages());
        templatesResponse.setLast(notifications.isLast());
        return templatesResponse;
    }
    public List<NotificationDto> getChatNotificationsOfUser(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "User not found.");
        }
        List<Notification> notifications = notificationRepository.findByUserIdAndNotiType(userId, "CHAT_NOTIFY");
        List<NotificationDto> notificationDtos = notifications.stream()
                .map(notification -> modelMapper.map(notification, NotificationDto.class))
                .collect(Collectors.toList());

        return notificationDtos;
    }
    public void sendPushNotification(NotificationRequest request) {
        Long userId = request.getUserId();
        String title = request.getTitle();
        String body = request.getBody();
        // Lấy user
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            System.out.println("User not found, skipping notification for userId: " + userId);
            return;
        }

        User user = optionalUser.get();

        // Lấy token
        String expoToken = user.getExpoToken();
        if (expoToken == null || expoToken.isEmpty()) {
            System.out.println("Expo token not found for user " + userId);
            return;
        }

        // Gửi đến Expo
        String url = "https://exp.host/--/api/v2/push/send";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> data = new HashMap<>();
        data.put("screen", request.getScreen());

        if (request.getBookingId() != null) {
            data.put("bookingId", request.getBookingId());
        }
        if (request.getBookingDetailId() != null) {
            data.put("bookingDetailId", request.getBookingDetailId());
        }
        Map<String, Object> message = Map.of(
                "to", expoToken,
                "sound", "default",
                "title", title,
                "body", body,
                "data", data
        );
        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(message, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, httpRequest, String.class);
        //System.out.println("Expo Response: " + response.getBody());

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(body);
        notification.setScreen(request.getScreen());

        if (request.getBookingId() != null) {
            notification.setBookingId(request.getBookingId());
        }
        if (request.getNotiType() != null) {
            notification.setNotiType(request.getNotiType());
        }
        if (request.getBookingDetailId() != null) {
            notification.setBookingDetailId(request.getBookingDetailId());
        }
        notificationRepository.save(notification);
        messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/notifications", notification);
    }
    public void markAsReadByIds(List<Long> ids) {
        List<Notification> notifications = notificationRepository.findAllById(ids);

        if (notifications.isEmpty()) return; // Không có thông báo nào hợp lệ

        for (Notification notification : notifications) {
            notification.setRead(true);
        }

        notificationRepository.saveAll(notifications);
    }
    public void markAllAsReadByUser(Long userId) {
        List<Notification> notifications = notificationRepository.findUnreadNonChatNotifications(userId);
        for (Notification n : notifications) {
            n.setRead(true);
        }
        notificationRepository.saveAll(notifications);
    }
    public void markAllChatAsReadByUser(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseAndNotiType(userId, "CHAT_NOTIFY");
        for (Notification n : notifications) {
            n.setRead(true);
        }
        notificationRepository.saveAll(notifications);
    }



}

