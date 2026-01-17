package com.spring2025.vietchefs.configs;

import com.nimbusds.jwt.SignedJWT;
import com.spring2025.vietchefs.repositories.InvalidatedAccessTokenRepository;
import com.spring2025.vietchefs.security.JwtKeyProvider;
import com.spring2025.vietchefs.security.JwtTokenProvider;
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
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomJwtDecoder implements JwtDecoder {
    JwtKeyProvider jwtKeyProvider;
    JwtTokenProvider jwtTokenProvider;
    InvalidatedAccessTokenRepository invalidatedRepository;
    @NonFinal
    NimbusJwtDecoder nimbusJwtDecoder = null;
    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            // 1. Kiểm tra cấu trúc và chữ ký (Hàm validateToken của bạn)
            var isValid = jwtTokenProvider.validateToken(token);
            if (!isValid) {
                throw new JwtException("Token signature or format is invalid");
            }

            // 2. Lấy JTI từ token
            SignedJWT signedJWT = SignedJWT.parse(token);
            String jti = signedJWT.getJWTClaimsSet().getJWTID();

            // 3. KIỂM TRA BLACKLIST (Bổ sung phần này)
            // Nếu JTI tồn tại trong DB, nghĩa là user đã logout
            if (invalidatedRepository.existsById(jti)) {
                throw new JwtException("Token has been invalidated (User logged out)");
            }

            // 4. Khởi tạo decoder nếu cần và giải mã
            if (Objects.isNull(nimbusJwtDecoder)) {
                nimbusJwtDecoder = NimbusJwtDecoder.withPublicKey(jwtKeyProvider.getPublicKey())
                        .signatureAlgorithm(SignatureAlgorithm.RS256)
                        .build();
            }

            return nimbusJwtDecoder.decode(token);
        } catch (Exception e) {
            throw new JwtException(e.getMessage());
        }
    }
}
