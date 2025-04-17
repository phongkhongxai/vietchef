package com.spring2025.vietchefs.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class GoogleOAuth2Service {

    @Value("${oauth.google.client-id}")
    private String clientId;

    @Value("${oauth.google.client-secret}")
    private String clientSecret;

    @Value("${oauth.google.redirect-uri}")
    private String redirectUri;

    @Autowired
    private WebClient webClientBuilder;

    public Map<String, Object> getUserInfoFromCode(String code) {
        // 1. Exchange code for token using WebClient
        String tokenUrl = "https://oauth2.googleapis.com/token";

        // Build the WebClient request for exchanging the code to access token
        Map<String, Object> tokenResponse = webClientBuilder
                .post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("code=" + code +
                        "&client_id=" + clientId +
                        "&client_secret=" + clientSecret +
                        "&redirect_uri=" + redirectUri +
                        "&grant_type=authorization_code")
                .retrieve()
                .bodyToMono(Map.class)
                .block();  // .block() là chặn bất đồng bộ và lấy kết quả

        String accessToken = (String) tokenResponse.get("access_token");

        // 2. Get user info using WebClient
        String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";

        // Build the WebClient request for getting user info
        Map<String, Object> userInfoResponse = webClientBuilder
                .get()
                .uri(userInfoUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();  // .block() để chờ kết quả bất đồng bộ

        return userInfoResponse;
    }
}
