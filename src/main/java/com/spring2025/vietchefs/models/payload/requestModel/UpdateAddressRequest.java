package com.spring2025.vietchefs.models.payload.requestModel;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAddressRequest {
    @NotNull(message = "Update Address's id can not be blank!")
    private Long id;

    private String title;

    private String address;
}
