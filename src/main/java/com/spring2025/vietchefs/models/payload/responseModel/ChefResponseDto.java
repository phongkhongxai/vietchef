package com.spring2025.vietchefs.models.payload.responseModel;

import com.spring2025.vietchefs.models.payload.dto.UserDto;
import lombok.*;

import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChefResponseDto {
    private Long id;
    private UserDto user;
    private String bio;                 // Giới thiệu ngắn
    private String description;         // Mô tả chi tiết
    private String address;
    private String country;
    private BigDecimal price;           // Giá theo giờ
    private Integer maxServingSize;     // Số lượng khách tối đa
    private String specialization;      // Chuyên môn
    private Integer yearsOfExperience;  // Số năm kinh nghiệm
    private String certification;       // Chứng chỉ nấu ăn
    private String status;              // Trạng thái ("PENDING", "ACTIVE", "INACTIVE")
}
