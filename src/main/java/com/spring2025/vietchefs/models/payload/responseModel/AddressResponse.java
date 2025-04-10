package com.spring2025.vietchefs.models.payload.responseModel;

import com.spring2025.vietchefs.models.payload.dto.UserDto;
import jakarta.persistence.Column;
import lombok.Data;

@Data
public class AddressResponse {
    private Long id;
    private String title;
    private String address;
    private Double latitude;
    private Double longitude;
}
