package com.spring2025.vietchefs.models.payload.requestModel;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateAddressRequest {
    private String title;

    @NotBlank(message = "Address can not be blank!")
    private String address;
}
