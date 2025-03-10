package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "chefs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Chef {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String bio;
    private String description;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private BigDecimal price;
    @Column(nullable = false)
    private Integer maxServingSize = 10;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private Boolean isDeleted = false;
    @Column(nullable = false)
    private String specialization = "Vietnamese Cuisine"; // Mặc định là "Ẩm thực Việt Nam"

    @Column(nullable = false)
    private String country; // Quốc gia hiện tại của đầu bếp (VD: USA, Canada, Germany)

    @Column(nullable = true)
    private Integer yearsOfExperience; // Số năm kinh nghiệm nấu món Việt

    @Column(nullable = true)
    private String certification; // Chứng chỉ ẩm thực (nếu có)
    @Column(nullable = false)
    private Boolean providesIngredients = false; // Có thể tự mua nguyên liệu không?

    @Column(nullable = true)
    private String preferredDishes; // Các món Việt Nam sở trường (VD: "Phở, Bún Bò, Cơm Tấm")

    @OneToMany(mappedBy = "chef", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChefSchedule> schedules;
}
