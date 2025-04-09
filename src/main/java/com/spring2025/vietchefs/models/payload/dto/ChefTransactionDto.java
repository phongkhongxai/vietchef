package com.spring2025.vietchefs.models.payload.dto;

import com.spring2025.vietchefs.models.entity.BookingDetail;
import com.spring2025.vietchefs.models.entity.Wallet;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ChefTransactionDto {
    private Long id;
    private Long walletId;
    private Long bookingDetailId;
    private String transactionType;
    private BigDecimal amount;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;
}
