package com.spring2025.vietchefs.services.impl;


import com.spring2025.vietchefs.models.entity.RefreshToken;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.LogoutRequest;
import com.spring2025.vietchefs.repositories.RefreshTokenRepository;
import com.spring2025.vietchefs.security.JwtTokenProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogoutService {
    RefreshTokenRepository refreshTokenRepository;
    JwtTokenProvider jwtTokenProvider;

    //    @Override
//    public void logout(
//            HttpServletRequest request,
//            HttpServletResponse response,
//            Authentication authentication
//    ) {
//        final String authHeader = request.getHeader("Authorization");
//        final String jwt;
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            return;
//        }
//        jwt = authHeader.substring(7);
//        AccessToken storedToken = accessTokenRepository.findByToken(jwt);
//        RefreshToken refreshToken = storedToken.getRefreshToken();
//        if (refreshToken != null) {
//            refreshToken.setExpired(true);
//            refreshToken.setRevoked(true);
//            refreshTokenRepository.save(refreshToken);
//        }
//        if (storedToken != null) {
//            storedToken.setRevoked(true);
//            storedToken.setExpired(true);
//            accessTokenRepository.save(storedToken);
//        }
//    }
    public void logout(LogoutRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Refresh token not found"));
        if (token.isRevoked() || token.isExpired()) {
            return;
        }
        token.setExpired(true);
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

}
