package com.spring2025.vietchefs.models.payload.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImageDto {
    private Long id;
    private String imageUrl;
    private String entityType;
    private Long entityId;
}
