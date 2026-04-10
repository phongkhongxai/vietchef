package com.spring2025.vietchefs.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring2025.vietchefs.models.payload.responseModel.ApiResponse;
import com.spring2025.vietchefs.services.impl.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RateLimitFilter extends OncePerRequestFilter {
    RateLimitService rateLimitService;
    RateLimitKey rateLimitKey;
    ClientKeyResolver clientKeyResolver;
    ObjectMapper objectMapper;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if(isSensitiveEndpoint(path)){
            String clientKey = clientKeyResolver.resolve(request);
            String baseKey = rateLimitKey.base(request.getMethod(), path, clientKey);

            boolean allowed = rateLimitService.checkAndConsume(
                    baseKey,
                    50,
                    Duration.ofMinutes(1)
            );

            if (!allowed){
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                ApiResponse<?> apiResponse = ApiResponse.builder()
                        .code(429)
                        .message("Too many requests.")
                        .build();
                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                response.flushBuffer();
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isSensitiveEndpoint(String path) {
        return path.contains("/no-auth/login")
                || path.contains("/no-auth/register")
                || path.contains("/no-auth/signin")
                || path.contains("/no-auth/resend-code")
                || path.contains("/api/v1/address/my-addresses-v2")
                || path.contains("/no-auth/forgot-password");
    }
}
