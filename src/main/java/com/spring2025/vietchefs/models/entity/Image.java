package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "images")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl;  // Link ảnh trong Azure Blob Storage

    @Column(nullable = false)
    private String entityType;  // Loại entity (DISH, USER, CHEF,...)

    @Column(nullable = false)
    private Long entityId;  // ID của entity liên quan
}
