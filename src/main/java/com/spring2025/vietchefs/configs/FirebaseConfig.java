package com.spring2025.vietchefs.configs;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
//huhuh
@Configuration
public class FirebaseConfig {
    @Bean
    public FirebaseApp initializeFirebaseApp() throws IOException {
        ClassPathResource serviceAccountResource = new ClassPathResource("secure-grammar-456201-n8-firebase-adminsdk-fbsvc-cd3a329fcb.json");

        try (InputStream serviceAccount = serviceAccountResource.getInputStream()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            return FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            throw new IOException("Không thể khởi tạo Firebase, tệp service account không tìm thấy hoặc không hợp lệ.", e);
        }
    }
}