package com.spring2025.vietchefs.models.payload.dto;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
@Getter
@Setter
public class NotificationDto {
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private boolean isRead = false;
    private LocalDateTime createdAt;
    private Long bookingId;
    private Long bookingDetailId;
    private String screen;
}
