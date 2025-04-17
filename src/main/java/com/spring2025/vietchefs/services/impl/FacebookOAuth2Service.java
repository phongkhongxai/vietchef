package com.spring2025.vietchefs.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class FacebookOAuth2Service {

    @Value("${oauth.facebook.client-id}")
    private String clientId;

    @Value("${oauth.facebook.client-secret}")
    private String clientSecret;

    @Value("${oauth.facebook.redirect-uri}")
    private String redirectUri;

    @Autowired
    private WebClient webClientBuilder;

    public Map<String, Object> getUserInfoFromCode(String code) {
        // 1. Exchange code for token using WebClient
        String tokenUrl = "https://graph.facebook.com/v12.0/oauth/access_token";
        
        // Build the WebClient request for exchanging the code to access token
        Map<String, Object> tokenResponse = webClientBuilder
                .post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("code=" + code +
                        "&client_id=" + clientId +
                        "&client_secret=" + clientSecret +
                        "&redirect_uri=" + redirectUri)
                .retrieve()
                .bodyToMono(Map.class)
                .block();  // .block() là chặn bất đồng bộ và lấy kết quả

        String accessToken = (String) tokenResponse.get("access_token");

        // 2. Get user info using WebClient
        String userInfoUrl = "https://graph.facebook.com/me?fields=id,name,email,picture";

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
