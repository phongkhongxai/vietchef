package com.spring2025.vietchefs.models.payload.dto;

import com.spring2025.vietchefs.models.entity.ChefTransaction;
import com.spring2025.vietchefs.models.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalletDto {
    private Long id;
    private Long userId;
    private BigDecimal balance;
    private String walletType;
}
