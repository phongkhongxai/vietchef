package com.spring2025.vietchefs.configs;

import com.nimbusds.jwt.SignedJWT;
import com.spring2025.vietchefs.repositories.InvalidatedAccessTokenRepository;
import com.spring2025.vietchefs.security.JwtKeyProvider;
import com.spring2025.vietchefs.security.JwtTokenProvider;
import com.spring2025.vietchefs.services.impl.TokenBlacklistService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPublicKey;
import java.util.Objects;

@Component
public class CustomJwtDecoder implements JwtDecoder {
    private final JwtDecoder jwtDecoder;
    private final TokenBlacklistService tokenBlacklistService;

    public CustomJwtDecoder(JwtKeyProvider jwtKeyProvider, TokenBlacklistService tokenBlacklistService) {
        this.jwtDecoder = NimbusJwtDecoder.withPublicKey(jwtKeyProvider.getPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        if (tokenBlacklistService.isBlacklisted(token)) {
            throw new JwtException("Token has been revoked (blacklisted)");
        }
        return jwtDecoder.decode(token);
    }
}
