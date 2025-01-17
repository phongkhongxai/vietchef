package com.spring2025.vietchefs.models.payload.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {
    @NotEmpty(message = "Username or email cannot be empty")
    private String usernameOrEmail;
    @NotEmpty(message = "Password cannot be empty")
    private String password;

}
