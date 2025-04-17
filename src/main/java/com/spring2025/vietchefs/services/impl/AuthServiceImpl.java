package com.spring2025.vietchefs.services.impl;



import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
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
import com.spring2025.vietchefs.services.WalletService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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
    private WalletService walletService;

    @Autowired
    public AuthServiceImpl(AuthenticationManager authenticationManager, UserRepository userRepository,
                           RoleRepository roleRepository, AccessTokenRepository accessTokenRepository, RefreshTokenRepository refreshTokenRepository, UserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider, ModelMapper modelMapper, EmailVerificationService emailVerificationService,WalletService walletService) {
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
        this.walletService = walletService;
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
        if (loginDto.getExpoToken() != null && !loginDto.getExpoToken().isBlank()) {
            user.setExpoToken(loginDto.getExpoToken());
            userRepository.save(user);
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

//    @Override
//    public AuthenticationResponse authenticateWithGoogle(String idToken) throws Exception {
//        // Xác thực idToken từ Firebase
//        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
//        String uid = decodedToken.getUid();
//        String email = decodedToken.getEmail();
//
//        if (email == null || uid == null) {
//            throw new Exception("Email or UID from Google is missing.");
//        }
//        // Nếu user đã tồn tại theo uid, dùng lại user
//        User user = userRepository.findByUid(uid).orElse(null);
//        if (user == null) {
//            // Nếu email đã tồn tại nhưng không cùng uid, báo lỗi (tránh bị override người khác)
//            if (userRepository.existsByEmail(email)) {
//                throw new VchefApiException(HttpStatus.BAD_REQUEST, "Email already exists with another account!");
//            }
//
//            // Lấy role mặc định cho người dùng Google
//            Role userRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
//                    .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Default role not found."));
//
//            // Tạo user mới từ thông tin Google
//            user = User.builder()
//                    .uid(uid)
//                    .email(email)
//                    .fullName(decodedToken.getName() != null ? decodedToken.getName() : "Unknown")
//                    .avatarUrl(decodedToken.getPicture())
//                    .username(generateUniqueUsername(email))
//                    .emailVerified(true)
//                    .role(userRole)
//                    .dob(LocalDate.now())
//                    .phone("default")
//                    .gender("default")
//                    .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Mật khẩu ngẫu nhiên
//                    .build();
//
//            userRepository.save(user);
//        }
//        // Tạo token cho user
//        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null);
//        String accessToken = jwtTokenProvider.generateAccessToken(authentication, user);
//        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication, user);
//
//        revokeRefreshToken(accessToken);
//        RefreshToken savedRefreshToken = saveUserRefreshToken(refreshToken);
//
//        revokeAllUserAccessTokens(user);
//        saveUserAccessToken(user, accessToken, savedRefreshToken);
//        return AuthenticationResponse.builder()
//                .accessToken(accessToken)
//                .refreshToken(refreshToken)
//                .fullName(user.getFullName())
//                .build();
//    }

//    @Override
//    public AuthenticationResponse authenticateWithFacebook(String accessToken) throws Exception {
//        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(accessToken);
//        String uid = decodedToken.getUid();
//        String email = decodedToken.getEmail();
//
//        if (email == null || uid == null) {
//            throw new Exception("Email or UID from Facebook is missing.");
//        }
//        // Nếu user đã tồn tại theo uid, dùng lại user
//        User user = userRepository.findByUid(uid).orElse(null);
//        if (user == null) {
//            // Nếu email đã tồn tại nhưng không cùng uid, báo lỗi (tránh bị override người khác)
//            if (userRepository.existsByEmail(email)) {
//                throw new VchefApiException(HttpStatus.BAD_REQUEST, "Email already exists with another account!");
//            }
//            Role userRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
//                    .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Default role not found."));
//
//            // Tạo user mới từ thông tin Facebook
//            user = User.builder()
//                    .uid(uid)
//                    .email(email)
//                    .fullName(decodedToken.getName() != null ? decodedToken.getName() : "Unknown")
//                    .avatarUrl(decodedToken.getPicture())
//                    .username(generateUniqueUsername(email))
//                    .emailVerified(true)
//                    .role(userRole)
//                    .dob(LocalDate.now())
//                    .phone("default")
//                    .gender("default")
//                    .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Mật khẩu ngẫu nhiên
//                    .build();
//
//            userRepository.save(user);
//        }
//
//        // Tạo token cho user
//        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null);
//        String accessTokenGenerated = jwtTokenProvider.generateAccessToken(authentication, user);
//        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication, user);
//
//        return AuthenticationResponse.builder()
//                .accessToken(accessTokenGenerated)
//                .refreshToken(refreshToken)
//                .fullName(user.getFullName())
//                .build();
//    }

    @Override
    public AuthenticationResponse authenticateWithOAuth2(String provider, Map<String, Object> userData) throws Exception {
        String email = (String) userData.get("email");
        String name = (String) userData.get("name");
        String picture;
        String uid;

        if (provider.equalsIgnoreCase("facebook")) {
            Map<String, Object> pictureData = (Map<String, Object>) ((Map<String, Object>) userData.get("picture")).get("data");
            picture = (String) pictureData.get("url");
            uid = "facebook_" + userData.get("id");
        } else if (provider.equalsIgnoreCase("google")) {
            picture = (String) userData.get("picture");
            uid = "google_" + userData.get("id");
        } else {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Unsupported provider: " + provider);
        }

        if (email == null || uid == null) {
            throw new Exception("Email or UID is missing from " + provider);
        }

        // Check if user exists
        User user = userRepository.findByUid(uid).orElse(null);
        if (user == null) {
            if (userRepository.existsByEmail(email)) {
                throw new VchefApiException(HttpStatus.BAD_REQUEST, "Email already exists with another account!");
            }

            Role userRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                    .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Default role not found."));

            user = User.builder()
                    .uid(uid)
                    .email(email)
                    .fullName(name != null ? name : "Unknown")
                    .avatarUrl(picture)
                    .username(generateUniqueUsername(email))
                    .emailVerified(true)
                    .role(userRole)
                    .dob(LocalDate.now())
                    .phone("default")
                    .gender("default")
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .build();

            userRepository.save(user);
        }

        // Generate tokens
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null);
        String accessToken = jwtTokenProvider.generateAccessToken(authentication, user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication, user);

        revokeRefreshToken(accessToken);
        RefreshToken savedRefreshToken = saveUserRefreshToken(refreshToken);

        revokeAllUserAccessTokens(user);
        saveUserAccessToken(user, accessToken, savedRefreshToken);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .fullName(user.getFullName())
                .build();
    }

    private String generateUniqueUsername(String email) {
        // Logic để tạo username duy nhất từ email, ví dụ: lấy phần email trước dấu "@"
        String username = email.split("@")[0];
        // Kiểm tra xem username đã tồn tại chưa
        while (userRepository.existsByUsername(username)) {
            username = username + System.currentTimeMillis(); // Thêm thời gian nếu username trùng
        }
        return username;
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
        walletService.createWallet(user1.getId(), "CUSTOMER");

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
                        .fullName(user.getFullName())
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
