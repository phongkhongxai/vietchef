package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.security.RateLimitKey;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RateLimitService {
    StringRedisTemplate redisTemplate;
    RateLimitKey rateLimitKey;
    @NonFinal
    String BLOCKED = "1";

    public boolean checkAndConsume(String baseKey, int max, Duration ttl) {
        if (redisTemplate.hasKey(rateLimitKey.block(baseKey))) {
            return false;
        }
        Long count = redisTemplate.opsForValue().increment(baseKey);

        if (count != null && count == 1) {
            redisTemplate.expire(baseKey, ttl);
        }
        if (count != null && count > max) {
            redisTemplate.opsForValue().set(rateLimitKey.block(baseKey), BLOCKED, ttl);
            return false;
        }
        return true;
    }
}
