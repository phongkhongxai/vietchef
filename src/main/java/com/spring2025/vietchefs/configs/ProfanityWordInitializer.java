package com.spring2025.vietchefs.configs;

import com.spring2025.vietchefs.services.ContentFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Lớp cấu hình để khởi tạo dữ liệu ban đầu cho từ ngữ cần lọc
 */
@Configuration
public class ProfanityWordInitializer {
    
    @Bean
    @Order(1) // Đảm bảo chạy sớm trong quá trình khởi động
    public CommandLineRunner initProfanityWords(ContentFilterService contentFilterService) {
        return args -> {
            // Khởi tạo danh sách từ ngữ lần đầu nếu database trống
            contentFilterService.initializeProfanityWordsIfEmpty();
        };
    }
} 