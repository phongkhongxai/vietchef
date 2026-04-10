package com.spring2025.vietchefs.services.impl;


import com.spring2025.vietchefs.models.entity.RefreshToken;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.LogoutRequest;
import com.spring2025.vietchefs.repositories.RefreshTokenRepository;
import com.spring2025.vietchefs.security.JwtTokenProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogoutService {
    RefreshTokenRepository refreshTokenRepository;
    TokenBlacklistService tokenBlacklistService;
    JwtTokenProvider jwtTokenProvider;

    public void logout(LogoutRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Refresh token not found"));
        if (!isValid(token)) {
            return;
        }
        token.setExpired(true);
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        if (request.getAccessToken() != null && !request.getAccessToken().isBlank()) {
            try {
                Instant expiryDate = jwtTokenProvider.getExpiration(request.getAccessToken());
                tokenBlacklistService.blacklistAccessToken(request.getAccessToken(), expiryDate);
            } catch (Exception e) {
                log.warn("Cannot blacklist access token during logout", e);
            }
        }
    }

    public boolean isValid(RefreshToken token) {
        return !token.isRevoked() && !token.isExpired()
                && token.getExpiryDate().isAfter(Instant.now());
    }

}
