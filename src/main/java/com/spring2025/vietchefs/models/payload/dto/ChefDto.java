package com.spring2025.vietchefs.models.payload.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChefDto {
    private Long id;
    private Long userId;
    private UserDto user;
    private String bio;
    private String description;
    private String address;
    private String country;
    private BigDecimal price;
    private Integer maxServingSize;
    private String status;
    private Integer reputationPoints;
}
