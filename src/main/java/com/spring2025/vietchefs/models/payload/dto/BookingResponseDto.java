package com.spring2025.vietchefs.models.payload.dto;


import com.spring2025.vietchefs.models.payload.responseModel.ChefResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.PackageResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {
    private Long id;
    private CustomerDto customer;
    private ChefResponseDto chef;
    private String bookingType;
    private String status;
    private String requestDetails;
    private int guestCount;
    private BigDecimal totalPrice;
    private PackageResponseDto bookingPackage;
    private List<BookingDetailDto> bookingDetails;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
