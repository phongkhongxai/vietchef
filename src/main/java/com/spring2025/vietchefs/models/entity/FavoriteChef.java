package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorite_chefs", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "chef_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteChef {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "chef_id", nullable = false)
    private Chef chef;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
} 