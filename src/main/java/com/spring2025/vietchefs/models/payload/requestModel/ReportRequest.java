package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.Data;

@Data
public class ReportRequest {
    private Long reportedChefId;
    private String reason;
    private String reasonDetail;
    private Long reviewId;         // Optional - nếu lý do liên quan đến feedback
    private Long bookingDetailId;  // (đặc biệt CHEF_NO_SHOW)
}