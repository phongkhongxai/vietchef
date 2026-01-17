package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
}

