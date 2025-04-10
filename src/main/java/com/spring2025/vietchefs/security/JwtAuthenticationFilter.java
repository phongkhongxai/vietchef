package com.spring2025.vietchefs.security;



import com.spring2025.vietchefs.models.entity.AccessToken;
import com.spring2025.vietchefs.repositories.AccessTokenRepository;
import com.spring2025.vietchefs.repositories.RefreshTokenRepository;
import com.spring2025.vietchefs.utils.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private AccessTokenRepository accessTokenRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

//    @Autowired
//    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
//                                   UserDetailsService userDetailsService,
//                                   AccessTokenRepository accessTokenRepository,
//                                   RefreshTokenRepository refreshTokenRepository) {
//        this.jwtTokenProvider = jwtTokenProvider;
//        this.userDetailsService = userDetailsService;
//        this.accessTokenRepository = accessTokenRepository;
//        this.refreshTokenRepository = refreshTokenRepository;
//    }

//    @Override
//    public void doFilterInternal(HttpServletRequest request,
//                                 HttpServletResponse response,
//                                 FilterChain filterChain) throws ServletException, IOException {
//        // get jwt from request header
//        String jwt = getJwtFromRequest(request);
//
//        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
//            // get username from jwt
//            String username = jwtTokenProvider.getUsernameFromJwt(jwt);
//            //String userId = jwtTokenProvider.getUserIdFromJwt(jwt);
//
//            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//                // load the user associated with the username from token
//                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//
//                AccessToken accessToken = accessTokenRepository.findByToken(jwt);
//
//                if (jwtTokenProvider.isTokenValid(jwt, userDetails.getUsername())
//                        && accessToken != null
//                        && !accessToken.isRevoked()
//                        && !accessToken.isExpired()
//                ) {
//                    UsernamePasswordAuthenticationToken authToken =
//                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//                    authToken.setDetails(
//                            new WebAuthenticationDetailsSource().buildDetails(request)
//                    );
//                    SecurityContextHolder.getContext().setAuthentication(authToken);
//                }
//            }
//        }
//
//        filterChain.doFilter(request, response);
//    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String jwt = getJwtFromRequest(request);
        if (path.equals("/no-auth/refresh-token")) {
            filterChain.doFilter(request, response);
            return;
        }
        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
            AccessToken accessToken = accessTokenRepository.findByToken(jwt);
            if (accessToken == null || accessToken.isRevoked() || accessToken.isExpired()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token has been revoked or expired");
                return;
            }


            String username = jwtTokenProvider.getUsernameFromJwt(jwt);
            String userIdStr = jwtTokenProvider.getUserIdFromJwt(jwt);
            Long userId = Long.parseLong(userIdStr);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                CustomUserDetails customUserDetails = new CustomUserDetails(
                        userId,
                        userDetails.getUsername(),
                        userDetails.getPassword(),
                        userDetails.getAuthorities()
                );

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
                // Sử dụng WebAuthenticationDetailsSource nếu cần để lưu các thông tin về request
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Lưu Authentication vào SecurityContextHolder
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
