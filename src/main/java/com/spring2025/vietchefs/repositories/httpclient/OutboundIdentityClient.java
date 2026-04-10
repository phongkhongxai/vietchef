package com.spring2025.vietchefs.repositories.httpclient;


import com.spring2025.vietchefs.models.payload.requestModel.ExchangeTokenRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ExchangeTokenResponse;
import feign.QueryMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "outbound-identity", url = "https://oauth2.googleapis.com")
public interface OutboundIdentityClient {
    @PostMapping(value = "/token", produces = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ExchangeTokenResponse exchangeToken(@QueryMap ExchangeTokenRequest request);
//    @PostMapping(
//            value = "/token",
//            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
//    )
//    ExchangeTokenResponse exchangeToken(Map<String, ?> request);
}
