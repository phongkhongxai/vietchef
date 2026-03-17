package com.spring2025.vietchefs.services.impl;



import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.spring2025.vietchefs.constant.PredefinedRole;
import com.spring2025.vietchefs.models.entity.RefreshToken;
import com.spring2025.vietchefs.models.entity.Role;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.LoginDto;
import com.spring2025.vietchefs.models.payload.dto.SignupDto;
import com.spring2025.vietchefs.models.payload.requestModel.ExchangeTokenRequest;
import com.spring2025.vietchefs.models.payload.requestModel.NewPasswordRequest;
import com.spring2025.vietchefs.models.payload.requestModel.RefreshRequest;
import com.spring2025.vietchefs.models.payload.responseModel.AuthenticationResponse;
import com.spring2025.vietchefs.repositories.RefreshTokenRepository;
import com.spring2025.vietchefs.repositories.RoleRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.repositories.httpclient.OutboundIdentityClient;
import com.spring2025.vietchefs.repositories.httpclient.OutboundUserClient;
import com.spring2025.vietchefs.security.JwtTokenProvider;
import com.spring2025.vietchefs.services.AuthService;
import com.spring2025.vietchefs.services.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final OutboundIdentityClient outboundIdentityClient;
    private final OutboundUserClient outboundUserClient;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationService emailVerificationService;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ModelMapper modelMapper;
    private final WalletService walletService;
    @Value("${app.jwt-refresh-expiration-seconds}")
    private long jwtRefreshExpiration;
    @Value("${outbound.identity.client-id}")
    protected String CLIENT_ID;
    @Value("${outbound.identity.client-secret}")
    protected String CLIENT_SECRET;
    @Value("${outbound.identity.redirect-uri}")
    protected String REDIRECT_URI;
    protected String GRANT_TYPE = "authorization_code";


    @Override
    public AuthenticationResponse login(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getUsernameOrEmail(), loginDto.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = userRepository.findByUsernameOrEmail(loginDto.getUsernameOrEmail(), loginDto.getUsernameOrEmail())
                .orElseThrow(() -> new VchefApiException(HttpStatus.BAD_REQUEST, "User not found"));
        if (!user.isEmailVerified()) {
            throw new VchefApiException(HttpStatus.FORBIDDEN, "Email is not verified. Please forgot password to continue.");
        }

        if (user.isBanned()) {
            throw new VchefApiException(HttpStatus.FORBIDDEN, "User is banned.");
        }

        if (loginDto.getExpoToken() != null && !loginDto.getExpoToken().isBlank()) {
            user.setExpoToken(loginDto.getExpoToken());
            userRepository.save(user);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);

        String fullName = user.getFullName();

        revokeRefreshToken(user.getId());
        RefreshToken savedRefreshToken = createRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(savedRefreshToken.getToken())
                .fullName(fullName)
                .build();
    }

    @Override
    public void updateTokenExpo(String email,String token) {
        Optional<User> user = userRepository.findByUsernameOrEmail(email,email);
        if (user.isEmpty()) return;
        if (token != null && !token.isBlank()) {
            user.get().setExpoToken(token);
            userRepository.save(user.get());
        }
    }

    @Override
    public AuthenticationResponse authenticateWithGoogle(String idToken) throws Exception {
        // Xác thực idToken từ Firebase
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        String uid = decodedToken.getUid();
        String email = decodedToken.getEmail();

        if (email == null || uid == null) {
            throw new Exception("Email or UID from Google is missing.");
        }
        // Nếu user đã tồn tại theo uid, dùng lại user
        User user = userRepository.findByUid(uid).orElse(null);
        if (user == null) {
            // Nếu email đã tồn tại nhưng không cùng uid, báo lỗi (tránh bị override người khác)
            if (userRepository.existsByEmail(email)) {
                throw new VchefApiException(HttpStatus.BAD_REQUEST, "Email already exists with another account!");
            }

            // Lấy role mặc định cho người dùng Google
            Role userRole = roleRepository.findByRoleName("CUSTOMER")
                    .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Default role not found."));

            // Tạo user mới từ thông tin Google
            user = User.builder()
                    .uid(uid)
                    .email(email)
                    .fullName(decodedToken.getName() != null ? decodedToken.getName() : "Unknown")
                    .avatarUrl(decodedToken.getPicture())
                    .username(generateUniqueUsername(email))
                    .emailVerified(true)
                    .role(userRole)
                    .dob(LocalDate.now())
                    .gender("default")
                    .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Mật khẩu ngẫu nhiên
                    .build();

            userRepository.save(user);
        }
        if (user.isBanned()) {
            throw new VchefApiException(HttpStatus.FORBIDDEN, "User is banned.");
        }
        // Tạo token cho user
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null);
        String accessToken = jwtTokenProvider.generateAccessToken(user);

        String fullName = user.getFullName();

        revokeRefreshToken(user.getId());
        RefreshToken savedRefreshToken = createRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(savedRefreshToken.getToken())
                .fullName(fullName)
                .build();
    }

    public AuthenticationResponse outboundAuthenticate(String code, String codeVerifier){
        var response = outboundIdentityClient.exchangeToken(ExchangeTokenRequest.builder()
                .code(code)
                .codeVerifier(codeVerifier)
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .redirectUri(REDIRECT_URI)
                .grantType(GRANT_TYPE)
                .build());

        log.info("TOKEN RESPONSE {}", response);

        // Get user info
        var userInfo = outboundUserClient.getUserInfo("json", response.getAccessToken());

        log.info("User Info {}", userInfo);

        Set<Role> roles = new HashSet<>();
        roles.add(Role.builder().roleName(PredefinedRole.CUSTOMER_ROLE).build());
        // Onboard user
//        var user = userRepository.findByUsername(userInfo.getEmail()).orElseGet(
//                () -> userRepository.save(User.builder()
//                        .username(userInfo.getEmail())
//                        .firstName(userInfo.getGivenName())
//                        .lastName(userInfo.getFamilyName())
//                        .roles(roles)
//                        .build()));
        Role userRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Default role not found."));
        var user = userRepository.findByEmail(userInfo.getEmail()).orElseGet(() -> {
            User newUser = User.builder()
                    .email(userInfo.getEmail())
                    .username(generateUniqueUsername(userInfo.getEmail()))
                    .fullName(userInfo.getName())
                    .avatarUrl(userInfo.getPicture())
                    .emailVerified(true)
                    .role(userRole)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .dob(LocalDate.now())
                    .gender("default")
                    .build();

            return userRepository.save(newUser);
        });
        walletService.createWallet(user.getId(), "CUSTOMER");
        if (user.isBanned()) {
            throw new VchefApiException(HttpStatus.FORBIDDEN, "User is banned.");
        }
        // Generate token
        String accessToken = jwtTokenProvider.generateAccessToken(user);

        String fullName = user.getFullName();

        revokeRefreshToken(user.getId());
        RefreshToken savedRefreshToken = createRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(savedRefreshToken.getToken())
                .fullName(fullName)
                .build();
    }

    @Override
    public AuthenticationResponse authenticateWithFacebook(String accessToken) throws Exception {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(accessToken);
        String uid = decodedToken.getUid();
        String email = decodedToken.getEmail();

        if (email == null || uid == null) {
            throw new Exception("Email or UID from Facebook is missing.");
        }
        // Nếu user đã tồn tại theo uid, dùng lại user
        User user = userRepository.findByUid(uid).orElse(null);
        if (user == null) {
            // Nếu email đã tồn tại nhưng không cùng uid, báo lỗi (tránh bị override người khác)
            if (userRepository.existsByEmail(email)) {
                throw new VchefApiException(HttpStatus.BAD_REQUEST, "Email already exists with another account!");
            }
            Role userRole = roleRepository.findByRoleName("CUSTOMER")
                    .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Default role not found."));

            // Tạo user mới từ thông tin Facebook
            user = User.builder()
                    .uid(uid)
                    .email(email)
                    .fullName(decodedToken.getName() != null ? decodedToken.getName() : "Unknown")
                    .avatarUrl(decodedToken.getPicture())
                    .username(generateUniqueUsername(email))
                    .emailVerified(true)
                    .role(userRole)
                    .dob(LocalDate.now())
                    .gender("default")
                    .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Mật khẩu ngẫu nhiên
                    .build();

            userRepository.save(user);
        }

        // Tạo token cho user
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null);
        String accessTokenGenerated = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = createRefreshToken(user).getToken();

        return AuthenticationResponse.builder()
                .accessToken(accessTokenGenerated)
                .refreshToken(refreshToken)
                .fullName(user.getFullName())
                .build();
    }

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
        User user = userRepository.findByUid(uid).orElse(null);
        if (user == null) {
            Optional<User> userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isPresent()) {
                user = userByEmail.get();
                user.setUid(uid);
                userRepository.save(user);
            } else {
                // Tạo user mới nếu không có ai trùng UID hay email
                Role userRole = roleRepository.findByRoleName("CUSTOMER")
                        .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Role not found."));

                user = User.builder()
                        .uid(uid)
                        .email(email)
                        .fullName(name != null ? name : "Unknown")
                        .avatarUrl(picture)
                        .username(generateUniqueUsername(email))
                        .emailVerified(true)
                        .role(userRole)
                        .dob(LocalDate.now())
                        .gender("default")
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .build();

                user = userRepository.save(user);
                walletService.createWallet(user.getId(), "CUSTOMER");
            }
        }

        // Generate tokens
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null);
        String accessToken = jwtTokenProvider.generateAccessToken(user);

        String fullName = user.getFullName();

        revokeRefreshToken(user.getId());
        RefreshToken savedRefreshToken = createRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(savedRefreshToken.getToken())
                .fullName(fullName)
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

    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(jwtTokenProvider.generateRefreshToken());
        refreshToken.setUser(user);
        refreshToken.setRevoked(false);
        refreshToken.setExpired(false);
        refreshToken.setExpiryDate(
                Instant.now().plus(jwtRefreshExpiration, ChronoUnit.SECONDS)
        );
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeRefreshToken(Long userId) {
        boolean exists = refreshTokenRepository.existsByUserId(userId);

        if (exists) {
            // 2. Chỉ thực hiện update khi thực sự có dữ liệu
            refreshTokenRepository.revokeAllByUserId(userId);
        }
    }

//    public void revokeAllUserAccessTokens(User user) {
//        var validUserTokens = accessTokenRepository.findAllValidTokensByUser(user.getId());
//        if (validUserTokens.isEmpty()) {
//            return;
//        }
//        validUserTokens.forEach(accessToken -> {
//            accessToken.setRevoked(true);
//            accessToken.setExpired(true);
//        });
//        accessTokenRepository.saveAll(validUserTokens);
//    }
//
//    private void saveUserAccessToken(User user, String jwtToken, RefreshToken refreshToken) {
//        var token = AccessToken.builder()
//                .user(user)
//                .token(jwtToken)
//                .refreshToken(refreshToken)
//                .revoked(false)
//                .expired(false)
//                .build();
//        accessTokenRepository.save(token);
//    }
//
//    private RefreshToken saveUserRefreshToken(String jwtToken) {
//        var token = RefreshToken.builder()
//                .token(jwtToken)
//                .revoked(false)
//                .expired(false)
//                .build();
//        return refreshTokenRepository.save(token);
//    }

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
        if (userRepository.existsByPhone(signupDto.getPhone())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Phone number is already exist!");
        }
        User user = modelMapper.map(signupDto, User.class);

        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        Role userRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User Role not found."));
        user.setRole(userRole);
        user.setEmailVerified(false);
        String avatarUrl = "https://api.dicebear.com/7.x/initials/svg?seed=" + signupDto.getUsername();
        user.setAvatarUrl(avatarUrl);
        emailVerificationService.sendVerificationCode(user);
        user = userRepository.save(user);
        walletService.createWallet(user.getId(), "CUSTOMER");
        return "Account registered successfully! Please check your email for the verification code.";
    }


    @Override
    public AuthenticationResponse refreshToken(RefreshRequest request) {
        String refreshTokenValue = request.getRefreshToken();

        if (refreshTokenValue == null || refreshTokenValue.isEmpty()) {
            throw new VchefApiException(HttpStatus.UNAUTHORIZED, "Refresh token missing in body");
        }
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new VchefApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new VchefApiException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
        }

        User user = refreshToken.getUser();

        // 1️⃣ Rotate refresh token (REVOKE OLD)
        refreshToken.setRevoked(true);
        refreshToken.setExpired(true);
        refreshTokenRepository.save(refreshToken);

        // 2️⃣ Create new refresh token
        RefreshToken newRefreshToken = createRefreshToken(user);

        // 3️⃣ Generate new access token
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);


        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .fullName(user.getFullName())
                .build();
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
    public String setPasswordAfterVerified(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new VchefApiException(HttpStatus.BAD_REQUEST, "User not found with this email."));

        if (!user.isEmailVerified()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Email has not been verified yet.");
        }
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        walletService.createWallet(user.getId(), "CUSTOMER");

        return "Password set successfully!";
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
        if(!user.isEmailVerified()){
            user.setEmailVerified(true);
            user.setVerificationCode(null);
            user.setVerificationCodeExpiry(null);
            userRepository.save(user);
        }

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
