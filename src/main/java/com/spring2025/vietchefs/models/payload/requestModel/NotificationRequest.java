package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
public class NotificationRequest {
    private Long userId;
    private String title;
    private String body;
    private Long bookingId;
    private Long bookingDetailId;
    private String screen;
    private String notiType;
}
