package com.spring2025.vietchefs.services.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class NotificationService {

    @Value("${firebase.server.key}")
    private String fcmServerKey;

    public void sendPushNotification(String targetToken, String title, String body) {
        String fcmUrl = "https://fcm.googleapis.com/fcm/send";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "key=" + fcmServerKey);

        Map<String, Object> notification = Map.of(
                "title", title,
                "body", body
        );

        Map<String, Object> message = Map.of(
                "to", targetToken,
                "notification", notification
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(message, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(fcmUrl, request, String.class);

        System.out.println("FCM Response: " + response.getBody());
    }
}

