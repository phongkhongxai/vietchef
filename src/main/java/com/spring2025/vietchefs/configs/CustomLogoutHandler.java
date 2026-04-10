package com.spring2025.vietchefs.configs;

import com.spring2025.vietchefs.models.entity.InvalidatedAccessToken;
import com.spring2025.vietchefs.repositories.InvalidatedAccessTokenRepository;
import com.spring2025.vietchefs.repositories.RefreshTokenRepository;
import com.spring2025.vietchefs.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import java.util.Date;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class CustomLogoutHandler implements LogoutHandler {
    private final InvalidatedAccessTokenRepository invalidatedAccessTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {
        final String authHeader = request.getHeader("Authorization");
        String refreshToken = request.getHeader("X-Refresh-Token");

        // 1. Xử lý Access Token (Blacklist)
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            try {
                String jti = jwtTokenProvider.extractId(accessToken);
                Date expiryDate = jwtTokenProvider.extractExpiration(accessToken);

                InvalidatedAccessToken blacklisted = new InvalidatedAccessToken(jti, expiryDate);
                invalidatedAccessTokenRepository.save(blacklisted);
            } catch (Exception e) {
                log.warn("Access token already expired or invalid, skipping blacklist");
            }
        }
        // 2. Xử lý Refresh Token (Vô hiệu hóa trong DB)
        if (refreshToken != null) {
            try {
                refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
                    if (!token.isRevoked() && !token.isExpired()) {
                        token.setExpired(true);
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                    }
                });
            } catch (Exception e) {
                log.error("Could not revoke refresh token", e);
            }
        }
    }
}
