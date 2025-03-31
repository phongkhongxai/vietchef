package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserRequest {
    private String fullName;
    private LocalDate dob;
    private String gender;
    private String phone;
    private String avatarUrl;
}
