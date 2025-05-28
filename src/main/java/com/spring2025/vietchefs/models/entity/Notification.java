package com.spring2025.vietchefs.models.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private boolean isRead = false;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    private Long bookingId;
    private Long bookingDetailId;
    private String screen; //nothing, booking, bookingDetail
    private String notiType;

}
