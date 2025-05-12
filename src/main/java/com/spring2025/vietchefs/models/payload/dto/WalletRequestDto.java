package com.spring2025.vietchefs.models.payload.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletRequestDto {
    private Long id;
    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotNull(message = "Request type is required")
    private String requestType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "2", message = "Amount must be greater than two.")
    private BigDecimal amount;

    private String status;

    private String note;
}
