package com.spring2025.vietchefs.models.payload.dto;

import com.spring2025.vietchefs.models.entity.BookingDetail;
import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.Package;
import com.spring2025.vietchefs.models.payload.responseModel.ChefResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.PackageResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {
    private Long id;
    private Long customerId;
    private ChefResponseDto chef;
    private String bookingType;
    private String status;
    private String requestDetails;
    private int guestCount;
    private BigDecimal totalPrice;
    private PackageResponseDto bookingPackage;
    private List<BookingDetailDto> bookingDetails;
}
