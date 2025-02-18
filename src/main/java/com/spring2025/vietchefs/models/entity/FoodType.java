package com.spring2025.vietchefs.models.entity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "food_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;  // Tên loại món ăn (ví dụ: "Món chay", "Món nướng", "Món hải sản")

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "foodType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Dish> dishes;
}
