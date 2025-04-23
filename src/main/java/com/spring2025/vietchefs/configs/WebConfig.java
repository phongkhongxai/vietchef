package com.spring2025.vietchefs.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] allowedOrigins;
        
        // Kiểm tra nếu đang chạy trên môi trường local
        if (isLocalEnvironment()) {
            // Cho phép tất cả origin khi chạy local
            allowedOrigins = new String[]{"http://localhost:3000", "http://localhost:8080"};  // Cập nhật các URL local nếu cần
        } else {
            // Chỉ cho phép domain sản xuất (production)
            allowedOrigins = new String[]{"https://vietchef.ddns.net"};
        }

        // Cấu hình CORS cho các request
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    // Hàm kiểm tra xem ứng dụng đang chạy trên môi trường local hay không
    private boolean isLocalEnvironment() {
        String env = System.getenv("ENV");
        return env == null || env.equals("local");
    }
}
