package com.spring2025.vietchefs.security;

import com.spring2025.vietchefs.utils.PemUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Component
public class JwtKeyProvider {

    @Value("${app.jwt.private-key-location}")
    private Resource privateKeyResource;

    @Value("${app.jwt.public-key-location}")
    private Resource publicKeyResource;

    public RSAPrivateKey getPrivateKey() {
        return (RSAPrivateKey) loadKey(privateKeyResource, true);
    }

    public RSAPublicKey getPublicKey() {
        return (RSAPublicKey) loadKey(publicKeyResource, false);
    }

    private Key loadKey(Resource resource, boolean isPrivate) {
        try {
            String key = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return isPrivate
                    ? PemUtils.readPrivateKey(key)
                    : PemUtils.readPublicKey(key);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load RSA key", e);
        }
    }
}

