package com.spring2025.vietchefs.services;



import com.google.firebase.auth.FirebaseAuthException;
import com.spring2025.vietchefs.models.payload.dto.LoginDto;
import com.spring2025.vietchefs.models.payload.dto.SignupDto;
import com.spring2025.vietchefs.models.payload.requestModel.NewPasswordRequest;
import com.spring2025.vietchefs.models.payload.responseModel.AuthenticationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface AuthService {
    AuthenticationResponse login(LoginDto loginDto);
    AuthenticationResponse authenticateWithGoogle(String idToken) throws Exception;
    AuthenticationResponse authenticateWithFacebook(String accessToken) throws Exception;
    String signup(SignupDto signupDto);
    AuthenticationResponse refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException;
    String verifyEmailCode(String email, String code);
    String resendVerificationCode(String email);
    String forgotPassword(String email);
    String resetPassword(NewPasswordRequest newPasswordRequest);
}
