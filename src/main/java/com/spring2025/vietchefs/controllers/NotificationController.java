package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.services.impl.NotificationService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notify")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody NotificationRequest request) {
        notificationService.sendPushNotification(request.getToken(), request.getTitle(), request.getBody());
        return ResponseEntity.ok("Sent");
    }
}

@Data
class NotificationRequest {
    private String token;
    private String title;
    private String body;
}
