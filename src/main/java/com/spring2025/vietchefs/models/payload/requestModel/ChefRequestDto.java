package com.spring2025.vietchefs.models.payload.requestModel;

import jakarta.persistence.Column;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChefRequestDto {
    private String bio;
    private String description;
    private String address;
    private BigDecimal price;
    private Integer maxServingSize;
    private String specialization; // Chuyên môn (Món Bắc, Món Nam, Món Trung, Hải sản...)
    private Integer yearsOfExperience; // Số năm kinh nghiệm
    private String certification; // Chứng chỉ nấu ăn (có thể là link hoặc tên chứng chỉ)
}
