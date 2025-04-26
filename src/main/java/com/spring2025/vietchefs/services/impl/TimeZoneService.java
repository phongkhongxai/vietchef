package com.spring2025.vietchefs.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class TimeZoneService {
    @Value("${google.maps.api.key}")
    private String googleApiKey;

    @Autowired
    private WebClient webClient;
    
    @Autowired
    private DistanceService distanceService;
    
    // Cache to avoid frequent API calls for the same address
    private final Map<String, String> addressToTimezoneCache = new HashMap<>();

    /**
     * Gets timezone ID from an address using Google APIs (Geocoding + Timezone)
     * @param address The address to find the timezone for
     * @return The timezone ID (e.g. "Asia/Ho_Chi_Minh")
     */
    public String getTimezoneFromAddress(String address) {
        try {
            // Check cache first
            if (addressToTimezoneCache.containsKey(address)) {
                return addressToTimezoneCache.get(address);
            }
            
            // Get coordinates from address using existing method
            double[] latLng = distanceService.getLatLngFromAddress(address);
            double lat = latLng[0];
            double lng = latLng[1];
            
            // Get current timestamp in seconds
            long timestamp = Instant.now().getEpochSecond();
            
            // Build Google Timezone API URL
            String url = "https://maps.googleapis.com/maps/api/timezone/json" +
                    "?location=" + lat + "," + lng +
                    "&timestamp=" + timestamp +
                    "&key=" + googleApiKey;
                    
            // Send request
            String response = webClient.get()
                    .uri(url)
                    .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                    
            // Parse JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response);
            
            // Check status
            if (!jsonNode.get("status").asText().equals("OK")) {
                System.err.println("⚠️ Timezone API error: " + jsonNode.get("status").asText());
                return ZoneId.systemDefault().getId(); // Fallback to system default
            }
            
            // Get timezone ID
            String timezoneId = jsonNode.get("timeZoneId").asText();
            
            // Cache the result
            addressToTimezoneCache.put(address, timezoneId);
            
            return timezoneId;
        } catch (Exception e) {
            System.err.println("❌ Error getting timezone: " + e.getMessage());
            return ZoneId.systemDefault().getId(); // Fallback to system default
        }
    }
    
    /**
     * Converts time from one timezone to another
     * @param localDateTime The local datetime to convert
     * @param sourceTimezone Source timezone ID
     * @param targetTimezone Target timezone ID
     * @return Converted LocalDateTime
     */
    public LocalDateTime convertBetweenTimezones(LocalDateTime localDateTime, String sourceTimezone, String targetTimezone) {
        try {
            // Create a ZonedDateTime from the LocalDateTime and source timezone
            ZonedDateTime sourceZoned = localDateTime.atZone(ZoneId.of(sourceTimezone));
            
            // Convert to target timezone
            ZonedDateTime targetZoned = sourceZoned.withZoneSameInstant(ZoneId.of(targetTimezone));
            
            // Return as LocalDateTime
            return targetZoned.toLocalDateTime();
        } catch (Exception e) {
            System.err.println("❌ Error converting timezone: " + e.getMessage());
            return localDateTime; // Return original time on error
        }
    }
} 