package com.spring2025.vietchefs.models.payload.requestModel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OutboundAuthRequest {
    private String code;
    @JsonProperty("code_verifier")
    private String codeVerifier;
}

