package com.spring2025.vietchefs.security;


import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {
    @Value("${app.jwt-secret}")
    private String jwtSecret;

    @Value("${app.jwt-access-expiration-milliseconds}")
    private long jwtAccessExpiration;

    @Value("${app.jwt-refresh-expiration-milliseconds}")
    private long jwtRefreshExpiration;

    public String generateAccessToken(Authentication authentication, User user) {
        return generateToken(authentication, user, jwtAccessExpiration);
    }

    public String generateRefreshToken(Authentication authentication, User user) {
        String token = generateToken(authentication, user, jwtRefreshExpiration);
        return token;
    }
    public String generateToken(Authentication authentication, User user, long expiration) {
        String username = authentication.getName();
        String userId = user.getId().toString();
        String role = user.getRole().getRoleName();

        Date currentDate = new Date();
        Date expirationDate = new Date(currentDate.getTime() + expiration);
        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(currentDate)
                .setExpiration(expirationDate)
                .claim("userId", userId)
                .claim("roleName", role)
                .claim("avatarUrl", user.getAvatarUrl());
        if (user.getChef() != null) {
            builder.claim("chefId", user.getChef().getId());
        }
        return builder
                .signWith(SignatureAlgorithm.HS384, key())
                .compact();

    }

    private Key key() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtSecret)
        );
    }

    public String getUsernameFromJwt(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
        String username = claims.getSubject();
        return username;
    }

    public String getUserIdFromJwt(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parse(token);
            return true;
        } catch (MalformedJwtException e) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Invalid JWT token");
        } catch (ExpiredJwtException e) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Expired JWT token");
        } catch (UnsupportedJwtException e) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "JWT claims string is empty");
        }
    }

    public boolean isTokenValid(String token, String checkUsername) {
        final String username = getUsernameFromJwt(token);
        return (username.equals(checkUsername) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration().before(new Date());
    }
}
