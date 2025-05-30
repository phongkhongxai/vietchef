package com.spring2025.vietchefs.models.payload.requestModel;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChefResponseRequest {
    @NotBlank
    private String response;
}
