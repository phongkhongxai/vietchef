package com.spring2025.vietchefs.services.impl;



import com.spring2025.vietchefs.models.entity.AccessToken;
import com.spring2025.vietchefs.models.entity.RefreshToken;
import com.spring2025.vietchefs.models.entity.Role;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.LoginDto;
import com.spring2025.vietchefs.models.payload.dto.SignupDto;
import com.spring2025.vietchefs.models.payload.requestModel.NewPasswordRequest;
import com.spring2025.vietchefs.models.payload.responseModel.AuthenticationResponse;
import com.spring2025.vietchefs.repositories.AccessTokenRepository;
import com.spring2025.vietchefs.repositories.RefreshTokenRepository;
import com.spring2025.vietchefs.repositories.RoleRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.security.JwtTokenProvider;
import com.spring2025.vietchefs.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {
    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private AccessTokenRepository accessTokenRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private EmailVerificationService emailVerificationService;
    private UserDetailsService userDetailsService;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private ModelMapper modelMapper;

    @Autowired
    public AuthServiceImpl(AuthenticationManager authenticationManager, UserRepository userRepository,
                           RoleRepository roleRepository, AccessTokenRepository accessTokenRepository, RefreshTokenRepository refreshTokenRepository, UserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider, ModelMapper modelMapper, EmailVerificationService emailVerificationService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.modelMapper = modelMapper;
        this.emailVerificationService = emailVerificationService;
    }

    @Override
    public AuthenticationResponse login(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getUsernameOrEmail(), loginDto.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = userRepository.findByUsernameOrEmail(loginDto.getUsernameOrEmail(), loginDto.getUsernameOrEmail())
                .orElseThrow(() -> new VchefApiException(HttpStatus.BAD_REQUEST, "User not found"));
        if (!user.isEmailVerified()) {
            throw new VchefApiException(HttpStatus.FORBIDDEN, "Email is not verified. Please verify your email.");
        }
        String accessToken = jwtTokenProvider.generateAccessToken(authentication, user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication, user);

        String fullName = user.getFullName();

        revokeRefreshToken(accessToken);
        RefreshToken savedRefreshToken = saveUserRefreshToken(refreshToken);

        revokeAllUserAccessTokens(user);
        saveUserAccessToken(user, accessToken, savedRefreshToken);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .fullName(fullName)
                .build();
    }

    public void revokeRefreshToken(String accessToken) {
        AccessToken token = accessTokenRepository.findByToken(accessToken);
        if (token != null) {
            RefreshToken refreshToken = token.getRefreshToken();
            refreshToken.setRevoked(true);
            refreshToken.setExpired(true);
            refreshTokenRepository.save(refreshToken);
        }
    }

    public void revokeAllUserAccessTokens(User user) {
        var validUserTokens = accessTokenRepository.findAllValidTokensByUser(user.getId());
        if (validUserTokens.isEmpty()) {
            return;
        }
        validUserTokens.forEach(accessToken -> {
            accessToken.setRevoked(true);
            accessToken.setExpired(true);
        });
        accessTokenRepository.saveAll(validUserTokens);
    }

    private void saveUserAccessToken(User user, String jwtToken, RefreshToken refreshToken) {
        var token = AccessToken.builder()
                .user(user)
                .token(jwtToken)
                .refreshToken(refreshToken)
                .revoked(false)
                .expired(false)
                .build();
        accessTokenRepository.save(token);
    }

    private RefreshToken saveUserRefreshToken(String jwtToken) {
        var token = RefreshToken.builder()
                .token(jwtToken)
                .revoked(false)
                .expired(false)
                .build();
        return refreshTokenRepository.save(token);
    }

    @Override
    @Transactional
    public String signup(SignupDto signupDto) {

        // add check if username already exists
        if (userRepository.existsByUsername(signupDto.getUsername())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Username is already exist!");
        }

        // add check if email already exists
        if (userRepository.existsByEmail(signupDto.getEmail())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Email is already exist!");
        }

        User user = modelMapper.map(signupDto, User.class);

        user.setPassword(passwordEncoder.encode(signupDto.getPassword()));

        Role userRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User Role not found."));
        user.setRole(userRole);
        user.setEmailVerified(false);
        user.setAvatarUrl("default");
        emailVerificationService.sendVerificationCode(user);

        User user1 = userRepository.save(user);

        return "User registered successfully! Please check your email for the verification code.";
    }
    @Override
    @Transactional
    public String signupOwner(SignupDto signupDto) {

        // add check if username already exists
        if (userRepository.existsByUsername(signupDto.getUsername())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Username is already exist!");
        }

        // add check if email already exists
        if (userRepository.existsByEmail(signupDto.getEmail())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Email is already exist!");
        }

        User user = modelMapper.map(signupDto, User.class);

        user.setPassword(passwordEncoder.encode(signupDto.getPassword()));

        Role userRole = roleRepository.findByRoleName("ROLE_OWNER")
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User Role not found."));
        user.setRole(userRole);
        user.setEmailVerified(false);
        user.setAvatarUrl("default");
        emailVerificationService.sendVerificationCode(user);

        User user1 = userRepository.save(user);

        return "User registered successfully! Please check your email for the verification code.";
    }


    @Override
    public AuthenticationResponse refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        refreshToken = authHeader.substring(7);
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken).orElseThrow();
        username = jwtTokenProvider.getUsernameFromJwt(refreshToken);

        if (username != null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Fetch the User entity
            User user = this.userRepository.findByUsernameOrEmail(username, username)
                    .orElseThrow(() -> new VchefApiException(HttpStatus.BAD_REQUEST, "User not found"));

            if (!token.isRevoked() && !token.isExpired()) {
                // Map user to authentication
                Authentication userAuthentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                // Generate access token with user details
                String accessToken = jwtTokenProvider.generateAccessToken(userAuthentication, user);

                // Revoke previous access tokens and save the new one
                revokeAllUserAccessTokens(user);
                saveUserAccessToken(user, accessToken, token);

                return AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
            } else {
                throw new VchefApiException(HttpStatus.BAD_REQUEST, "Invalid refresh token");
            }
        }
        return null;
    }

    @Override
    public String verifyEmailCode(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new VchefApiException(HttpStatus.BAD_REQUEST, "User not found with this email."));

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Invalid verification code.");
        }

        if (user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Verification code has expired.");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null); // Clear the code after verification
        user.setVerificationCodeExpiry(null);
        userRepository.save(user);
        return "Email verified successfully!";
    }

    @Override
    public String resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new VchefApiException(HttpStatus.BAD_REQUEST, "User not found with this email."));

        if (user.isEmailVerified()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST,"Email is already verified.");
        }
        emailVerificationService.sendVerificationCode(user);
        return "Verification code resent to your email.";
    }

    @Override
    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new VchefApiException(HttpStatus.BAD_REQUEST, "User not found with this email."));

        emailVerificationService.sendPasswordResetToken(user);

        return "Password reset token sent to your email.";
    }

    @Override
    public String resetPassword(NewPasswordRequest newPasswordRequest) {
        User user = userRepository.findByResetPasswordTokenAndEmail(newPasswordRequest.getToken(), newPasswordRequest.getEmail())
                .orElseThrow(() -> new VchefApiException(HttpStatus.BAD_REQUEST, "Invalid or expired password reset token."));

        if (user.getResetPasswordExpiry().isBefore(LocalDateTime.now())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Password reset token has expired.");
        }

        // Update user's password
        user.setPassword(passwordEncoder.encode(newPasswordRequest.getNewPassword()));
        user.setResetPasswordToken(null); // Clear the reset token
        user.setResetPasswordExpiry(null);
        userRepository.save(user);

        return "Password reset successfully!";
    }

}
