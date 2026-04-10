package com.spring2025.vietchefs.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientKeyResolver {
    public String resolve(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // 1. Ưu tiên lấy IP từ header X-Forwarded-For (đề phòng chạy sau Proxy/Nginx)
        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            // 2. Nếu không có proxy, lấy trực tiếp từ RemoteAddr
            ipAddress = request.getRemoteAddr();
        } else {
            // 3. Nếu qua nhiều Proxy, lấy IP đầu tiên trong chuỗi (ip1, ip2, ip3)
            ipAddress = ipAddress.split(",")[0].trim();
        }

        // Xử lý trường hợp IPv6 của localhost
        if ("0:0:0:0:0:0:0:1".equals(ipAddress)) {
            ipAddress = "127.0.0.1";
        }

        return ipAddress;
    }
}
