package com.spring2025.vietchefs.models.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteChefDto {
    private Long id;
    private Long userId;
    private Long chefId;
    private String chefName;
    private String chefAvatar;
    private String chefSpecialization;
    private String chefAddress;
    private LocalDateTime createdAt;
} 