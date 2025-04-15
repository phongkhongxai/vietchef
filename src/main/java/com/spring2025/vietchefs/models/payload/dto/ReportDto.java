package com.spring2025.vietchefs.models.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportDto {
    private Long id;
    private String reportedByName;
    private String reportedChefName;
    private String reason;
    private String reasonDetail;
    private String status;
    private LocalDateTime createAt;
    private Long reviewId;
}
