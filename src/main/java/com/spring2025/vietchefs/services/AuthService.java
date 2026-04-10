package com.spring2025.vietchefs.services;



import com.spring2025.vietchefs.models.payload.dto.LoginDto;
import com.spring2025.vietchefs.models.payload.dto.SignupDto;
import com.spring2025.vietchefs.models.payload.requestModel.NewPasswordRequest;
import com.spring2025.vietchefs.models.payload.requestModel.RefreshRequest;
import com.spring2025.vietchefs.models.payload.responseModel.AuthenticationResponse;

import java.util.Map;

public interface AuthService {
    AuthenticationResponse login(LoginDto loginDto);
    void updateTokenExpo(String email,String token);
    AuthenticationResponse authenticateWithGoogle(String idToken) throws Exception;
    AuthenticationResponse outboundAuthenticate(String code, String codeVerifier);
    AuthenticationResponse authenticateWithFacebook(String accessToken) throws Exception;
    AuthenticationResponse authenticateWithOAuth2(String provider, Map<String, Object> userData) throws Exception;
    String signup(SignupDto signupDto);
    AuthenticationResponse refreshToken(RefreshRequest request);
    String verifyEmailCode(String email, String code);
//    String setPasswordAfterVerified(String email, String password);
    String resendVerificationCode(String email);
    String forgotPassword(String email);
    String resetPassword(NewPasswordRequest newPasswordRequest);
}
