package com.spring2025.vietchefs.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring2025.vietchefs.models.payload.responseModel.DistanceResponse;
import com.spring2025.vietchefs.models.payload.responseModel.GoogleDistanceResponse;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class DistanceService {
    @Value("${google.maps.api.key}")
    private String googleApiKey;

    @Autowired
    private WebClient webClient;

    public DistanceResponse calculateDistanceAndTime(String origin, String destination) {
        try {
            String originFormatted = origin.trim().replaceAll("\\s+", "+");
            String destinationFormatted = destination.trim().replaceAll("\\s+", "+");

            // Tạo URL trực tiếp cho Distance Matrix API
            String url = "https://maps.googleapis.com/maps/api/distancematrix/json" +
                    "?origins=" + originFormatted +
                    "&destinations=" + destinationFormatted +
                    "&units=metric" +
                    "&mode=driving" +
                    "&key=" + googleApiKey;

            System.out.println("🔗 URL gửi đi: " + url);

            // Gửi request
            String rawResponse = webClient.get()
                    .uri(url)
                    .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(e -> System.err.println("WebClient lỗi: " + e.getMessage()))
                    .block();

            if (rawResponse == null) {
                System.err.println("⚠️ Response null từ Google API!");
                return new DistanceResponse(BigDecimal.ZERO, BigDecimal.ZERO);
            }

            System.out.println("✅ API Response: " + rawResponse);

            // Parse JSON
            ObjectMapper mapper = new ObjectMapper();
            GoogleDistanceResponse response = mapper.readValue(rawResponse, GoogleDistanceResponse.class);

            // Kiểm tra response
            if ("OK".equals(response.getStatus()) && !response.getRows().isEmpty()) {
                GoogleDistanceResponse.Row row = response.getRows().get(0);
                if (!row.getElements().isEmpty()) {
                    GoogleDistanceResponse.Element element = row.getElements().get(0);
                    if ("OK".equals(element.getStatus()) && element.getDistance() != null && element.getDuration() != null) {
                        double distanceKm = element.getDistance().getValue() / 1000.0;
                        double durationHours = element.getDuration().getValue() / 3600.0;
                        System.out.println("📏 Distance: " + distanceKm + " km");
                        System.out.println("⏳ Duration: " + durationHours + " hours");
                        return new DistanceResponse(BigDecimal.valueOf(distanceKm), BigDecimal.valueOf(durationHours));
                    } else {
                        System.err.println("⚠️ Element Status: " + element.getStatus());
                    }
                }
            } else {
                System.err.println("⚠️ Response Status: " + response.getStatus());
            }

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi gọi Google API: " + e.getMessage());
            e.printStackTrace();
        }

        return new DistanceResponse(BigDecimal.ZERO, BigDecimal.ZERO);
    }
    public double[] getLatLngFromAddress(String address) {
        try {
            String encodedAddress = address.trim().replaceAll("\\s+", "+");
            String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" + encodedAddress + "&key=" + googleApiKey;

            // Sử dụng WebClient đã cấu hình
            String response = webClient.get()
                    .uri(url)
                    .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(e -> System.err.println("❌ WebClient lỗi: " + e.getMessage()))
                    .block();
//            System.out.println("🔍 Requesting address: " + url);
//            System.out.println("📦 Google Response: " + response);


            // Parse JSON với ObjectMapper
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response);

            // Kiểm tra status
            if (!jsonNode.get("status").asText().equals("OK")) {
                throw new RuntimeException("Địa chỉ không hợp lệ hoặc không tìm được.");
            }

            // Lấy tọa độ từ JSON response
            JsonNode locationNode = jsonNode.get("results").get(0).get("geometry").get("location");

            // Trả về latitude và longitude
            double lat = locationNode.get("lat").asDouble();
            double lng = locationNode.get("lng").asDouble();

            return new double[] { lat, lng };
        } catch (Exception e) {
            throw new RuntimeException("Không thể lấy tọa độ: " + e.getMessage(), e);
        }
    }


}
