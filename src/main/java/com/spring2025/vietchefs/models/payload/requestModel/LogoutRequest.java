package com.spring2025.vietchefs.models.payload.requestModel;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LogoutRequest {
    @NotBlank(message = "AccessToken is required")
    String accessToken;

    @NotBlank(message = "RefreshToken is required")
    String refreshToken;
}

