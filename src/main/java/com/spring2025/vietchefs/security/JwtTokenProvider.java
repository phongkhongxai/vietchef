package com.spring2025.vietchefs.security;


import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.utils.PemUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider {
//    @Value("${app.jwt-secret}")
//    private String jwtSecret;
    private final JwtKeyProvider keyProvider;

    @Value("${app.jwt-access-expiration-seconds}")
    private long jwtAccessExpiration;



//    public String generateAccessToken(User user) {
//        // Truyền expiration và định danh loại token là "ACCESS"
//        return generateToken(user, jwtAccessExpiration);
//    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString() + UUID.randomUUID();
    }
    public String generateAccessToken(User user) {
        // 1. Header
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .build();

        // 2. Build claims
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("vietchefs.com")
                .issueTime(new Date())
                .expirationTime(
                        new Date(Instant.now()
                                .plus(jwtAccessExpiration, ChronoUnit.SECONDS)
                                .toEpochMilli())
                )
                .jwtID(UUID.randomUUID().toString())
                .claim("userId", user.getId())
                .claim("scope", buildScope(user));

        // ChefId nếu tồn tại
        if (user.getChef() != null) {
            claimsBuilder.claim("chefId", user.getChef().getId());
        }

        JWTClaimsSet jwtClaimsSet = claimsBuilder.build();

        // 3. Payload
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        // 4. Ký JWT
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            RSASSASigner signer = new RSASSASigner(keyProvider.getPrivateKey());
            jwsObject.sign(signer);
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new VchefApiException(HttpStatus.FORBIDDEN,"Cannot generate JWT");
        }
    }

    public SignedJWT verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier verifier = new RSASSAVerifier(keyProvider.getPublicKey());
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
        Date expiryTime = claimsSet.getExpirationTime();
        var verified = signedJWT.verify(verifier);
        if (!(verified && expiryTime.after(new Date()))) {
            throw new VchefApiException(HttpStatus.UNAUTHORIZED,"Unauthenciated");
        }
        return signedJWT;
    }



    private String buildScope(User user) {
        if (user.getRole() == null) {
            return "";
        }
        return "ROLE_" + user.getRole().getRoleName();
    }

    public <T> T extractClaim(String token, Function<JWTClaimsSet, T> claimsResolver) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            JWSVerifier verifier = new RSASSAVerifier(keyProvider.getPublicKey());
            if (!signedJWT.verify(verifier)) {
                throw new VchefApiException(HttpStatus.UNAUTHORIZED, "Invalid JWT signature");
            }

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            return claimsResolver.apply(claimsSet);
        } catch (ParseException | JOSEException e) {
            log.error("Could not extract claims", e);
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Invalid token");
        }
    }
    public Instant getExpiration(String token) {
        if (token == null || token.isBlank()) {
            return Instant.now().minusSeconds(1); // coi như đã hết hạn
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            Date expiryDate = claimsSet.getExpirationTime();

            return (expiryDate != null)
                    ? expiryDate.toInstant()
                    : Instant.now().minusSeconds(1); // fallback: coi như hết hạn

        } catch (Exception e) {
            log.warn("Failed to extract expiration time from token", e);
            return Instant.now().minusSeconds(1); // coi như hết hạn nếu parse lỗi
        }
    }

    // 2. Lấy JTI (ID của token) - Dùng cho Logout Blacklist
    public String extractId(String token) {
        return extractClaim(token, JWTClaimsSet::getJWTID);
    }

    // 3. Lấy thời gian hết hạn
    public Date extractExpiration(String token) {
        return extractClaim(token, JWTClaimsSet::getExpirationTime);
    }

    // 4. Lấy Subject (Username)
    public String extractUsername(String token) {
        return extractClaim(token, JWTClaimsSet::getSubject);
    }


    public boolean validateToken(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);

            if (!jwt.verify(new RSASSAVerifier(keyProvider.getPublicKey()))) {
                log.warn("JWT signature invalid");
                return false;
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            if (claims.getExpirationTime() == null ||
                    claims.getExpirationTime().before(new Date())) {
                log.warn("JWT expired");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("JWT validation error", e);
            return false;
        }
    }


//    public boolean isTokenValid(String token, String checkUsername) {
//        final String username = getUsernameFromJwt(token);
//        return (username.equals(checkUsername) && !isTokenExpired(token));
//    }
//
//    private boolean isTokenExpired(String token) {
//        Claims claims = Jwts.parserBuilder()
//                .setSigningKey(key())
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//        return claims.getExpiration().before(new Date());
//    }
}
