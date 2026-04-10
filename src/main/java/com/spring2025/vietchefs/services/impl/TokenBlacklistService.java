package com.spring2025.vietchefs.services.impl;


import com.spring2025.vietchefs.security.JwtTokenProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenBlacklistService {

    StringRedisTemplate redisTemplate;
    JwtTokenProvider jwtTokenProvider;
    String BLACKLIST_PREFIX = "blacklist:access:";

    public void blacklistAccessToken(String token, Instant expiresAt) {
        if (token == null || expiresAt == null || expiresAt.isBefore(Instant.now())) {
            return;
        }

        String jti = jwtTokenProvider.extractId(token);
        String key = BLACKLIST_PREFIX + (jti != null ? jti : token);

        long ttlSeconds = Duration.between(Instant.now(), expiresAt).getSeconds();

        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(key, "revoked", ttlSeconds, TimeUnit.SECONDS);
        }
    }

    public boolean isBlacklisted(String token) {
        if (token == null) return false;
        String jti = jwtTokenProvider.extractId(token);
        String key = BLACKLIST_PREFIX + (jti != null ? jti : token);
        return redisTemplate.hasKey(key);
    }

    public boolean isBlacklistedJti(String jti) {
        if (jti == null) return false;
        String key = BLACKLIST_PREFIX + jti;
        return redisTemplate.hasKey(key);
    }
}
