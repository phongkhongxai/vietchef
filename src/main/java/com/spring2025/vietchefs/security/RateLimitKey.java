package com.spring2025.vietchefs.security;

import org.springframework.stereotype.Component;

@Component
public class RateLimitKey {
    public String base(String method, String path, String clientKey) {
        return String.format("rate-limit:%s:%s:%s",
                method.toUpperCase(),
                path,
                normalize(clientKey));


    }
    public String block(String baseKey){
        return baseKey +":blocked";
    }

    private String normalize(String key) {
        if (key == null) return "null";
        if (key.length() <= 20) return key;
        return Integer.toHexString(key.hashCode());
    }
}
