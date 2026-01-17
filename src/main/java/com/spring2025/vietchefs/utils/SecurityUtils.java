package com.spring2025.vietchefs.utils;

import com.spring2025.vietchefs.models.exception.VchefApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
    public static Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim != null) {
                userId = Long.valueOf(userIdClaim.toString());
            }
        }

        if (userId == null) {
            throw new VchefApiException(HttpStatus.FORBIDDEN, "Không tìm thấy UserId trong Token");
        }

        return userId;
    }
}
