package com.spring2025.vietchefs.controllers;


import com.spring2025.vietchefs.models.payload.dto.LoginDto;
import com.spring2025.vietchefs.models.payload.dto.SignupDto;
import com.spring2025.vietchefs.models.payload.requestModel.NewPasswordRequest;
import com.spring2025.vietchefs.models.payload.requestModel.SetPasswordDto;
import com.spring2025.vietchefs.models.payload.responseModel.AuthenticationResponse;
import com.spring2025.vietchefs.services.AuthService;
import com.spring2025.vietchefs.services.impl.FacebookOAuth2Service;
import com.spring2025.vietchefs.services.impl.GoogleOAuth2Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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
        return ResponseEntity.ok(token);
    }
    @GetMapping(value = {"/hello-wrds"})
    public ResponseEntity<String> login1(){
        String sadas = "sadas";
        return ResponseEntity.ok(sadas);
    }

    @PostMapping("/register")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupDto signupDto){
        String response = authService.signup(signupDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    @PostMapping("/set-password")
    public ResponseEntity<String> setPasword(@Valid @RequestBody SetPasswordDto setPasswordDto){
        String response = authService.setPasswordAfterVerified(setPasswordDto.getEmail(),setPasswordDto.getPassword());
        return new ResponseEntity<>(response, HttpStatus.OK);
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
//    @PostMapping("/google-login")
//    public ResponseEntity<?> loginWithGoogle(@RequestParam String idToken) {
//        try {
//            AuthenticationResponse token = authService.authenticateWithGoogle(idToken);
//            return ResponseEntity.ok(token);
//        } catch (Exception e) {
//            return ResponseEntity
//                    .status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("message: " +e.getMessage(), "Authentication failed: Invalid Firebase token"));
//        }
//    }
//
//    @PostMapping("/facebook-login")
//    public ResponseEntity<?> loginWithFb(@RequestParam String idToken) {
//        try {
//            AuthenticationResponse token = authService.authenticateWithFacebook(idToken);
//            return ResponseEntity.ok(token);
//        } catch (Exception e) {
//            return ResponseEntity
//                    .status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("message: " +e.getMessage(), "Authentication failed: Invalid Firebase token"));
//        }
//    }

    @Autowired
    private GoogleOAuth2Service googleOAuth2Service;
    @Autowired
    private FacebookOAuth2Service facebookOAuth2Service;

    @GetMapping("/google/callback")
    public void handleGoogleCallback(@RequestParam("code") String code, HttpServletResponse response) throws Exception {
        Map<String, Object> userInfo = googleOAuth2Service.getUserInfoFromCode(code);
        AuthenticationResponse authResponse = authService.authenticateWithOAuth2("google", userInfo);
        String fullNameEncoded = URLEncoder.encode(authResponse.getFullName(), StandardCharsets.UTF_8);
        String redirectUrl = "https://vietchef.ddns.net/no-auth/oauth-redirect"
                + "?access_token=" + authResponse.getAccessToken()
                + "&refresh_token=" + authResponse.getRefreshToken() +"&full_name="+fullNameEncoded;
        response.sendRedirect(redirectUrl);
    }
    @GetMapping("/oauth-redirect")
    public String handleOAuthRedirect(@RequestParam("access_token") String accessToken,
                                      @RequestParam("refresh_token") String refreshToken,
                                      @RequestParam("full_name") String fullName) {

        return "Đăng nhập thành công với " + fullName;
    }
    @PutMapping("/save-device-token")
    public ResponseEntity<Void> saveTokenDevice(@RequestParam String email, @RequestParam String token) {
        String decodedToken = URLDecoder.decode(token, StandardCharsets.UTF_8);
        authService.updateTokenExpo(email, decodedToken);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/facebook/callback")
    public ResponseEntity<?> handleFacebookCallback(@RequestParam("code") String code) throws Exception {
        Map<String, Object> userInfo = facebookOAuth2Service.getUserInfoFromCode(code);
        return ResponseEntity.ok(authService.authenticateWithOAuth2("facebook", userInfo));
    }
    @Value("${oauth.google.client-id}")
    private String googleClientId;

    @Value("${oauth.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${oauth.facebook.client-id}")
    private String facebookClientId;

    @Value("${oauth.facebook.redirect-uri}")
    private String facebookRedirectUri;

    @GetMapping("/oauth-url")
    public ResponseEntity<Map<String, String>> getOAuthUrl(@RequestParam("provider") String provider) {
        String oauthUrl;
        switch (provider.toLowerCase()) {
            case "google":
                oauthUrl = UriComponentsBuilder
                        .fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                        .queryParam("client_id", googleClientId)
                        .queryParam("redirect_uri", googleRedirectUri)
                        .queryParam("response_type", "code")
                        .queryParam("scope", "email profile")
                        .queryParam("access_type", "offline")
                        .queryParam("prompt", "select_account")
                        .build()
                        .toUriString();
                break;

            case "facebook":
                oauthUrl = UriComponentsBuilder
                        .fromHttpUrl("https://www.facebook.com/v12.0/dialog/oauth")
                        .queryParam("client_id", facebookClientId)
                        .queryParam("redirect_uri", facebookRedirectUri)
                        .queryParam("response_type", "code")
                        .queryParam("scope", "email,public_profile")
                        .build()
                        .toUriString();
                break;

            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported provider");
        }

        Map<String, String> response = new HashMap<>();
        response.put("url", oauthUrl);
        return ResponseEntity.ok(response);
    }


}
