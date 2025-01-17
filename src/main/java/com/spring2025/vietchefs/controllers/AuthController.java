package com.spring2025.vietchefs.controllers;


import com.spring2025.vietchefs.models.payload.dto.LoginDto;
import com.spring2025.vietchefs.models.payload.dto.SignupDto;
import com.spring2025.vietchefs.models.payload.requestModel.NewPasswordRequest;
import com.spring2025.vietchefs.models.payload.responseModel.AuthenticationResponse;
import com.spring2025.vietchefs.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/no-auth")
public class AuthController {
    private AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = {"/login", "/signin"})
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginDto loginDto){
        AuthenticationResponse token = authService.login(loginDto);
        return  ResponseEntity.ok(token);
    }

    @PostMapping("/register")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupDto signupDto){
        String response = authService.signup(signupDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/register-owner")
    public ResponseEntity<String> signupOwner(@Valid @RequestBody SignupDto signupDto){
        String response = authService.signupOwner(signupDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        return ResponseEntity.ok(authService.refreshToken(request, response));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyEmailCode(@RequestParam("email") String email,
                                                  @RequestParam("code") String code) {
        String response = authService.verifyEmailCode(email, code);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/resend-code")
    public ResponseEntity<String> resendVerificationCode(@RequestParam("email") String email) {
         String response = authService.resendVerificationCode(email);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam("email") String email) {
         String response = authService.forgotPassword(email);
        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody NewPasswordRequest newPasswordRequest) {
        String response = authService.resetPassword(newPasswordRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);

    }

}
