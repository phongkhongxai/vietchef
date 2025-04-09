package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Dish;
import com.spring2025.vietchefs.models.entity.Image;
import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.repositories.DishRepository;
import com.spring2025.vietchefs.repositories.ImageRepository;
import com.spring2025.vietchefs.repositories.ReviewRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ImageService {
    @Autowired
    private  AzureBlobStorageService azureBlobStorageService;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private DishRepository dishRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReviewRepository reviewRepository;

    public String uploadImage(MultipartFile file, Long entityId, String entityType) throws IOException {
        // Upload ảnh lên Azure Blob Storage
         String imageUrl = azureBlobStorageService.uploadFile(file);
        Image image = new Image();

        // Lưu thông tin ảnh vào cơ sở dữ liệu (ví dụ: món ăn, người dùng, review, v.v.)
        if ("DISH".equals(entityType)) {
            Optional<Dish> dishOptional = dishRepository.findById(entityId);
            if (dishOptional.isPresent()) {
                Dish dish = dishOptional.get();
                dish.setImageUrl(imageUrl);
                dishRepository.save(dish); // Cập nhật ảnh cho món ăn
                Image image1 = Image.builder()
                        .imageUrl(imageUrl) // Link ảnh
                        .entityType(entityType)
                        .entityId(entityId)
                        .build();

                image = imageRepository.save(image1);
            }else{
                throw new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found");
            }
        } else if ("USER".equals(entityType)) {
            // Nếu entity là người dùng
            Optional<User> userOptional = userRepository.findById(entityId);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setAvatarUrl(imageUrl);
                userRepository.save(user); // Cập nhật ảnh avatar cho người dùng
                 Image image1 = Image.builder()
                        .imageUrl(imageUrl) // Link ảnh
                        .entityType(entityType)
                        .entityId(entityId)
                        .build();
                image = imageRepository.save(image1);
            }else{
                throw new VchefApiException(HttpStatus.NOT_FOUND, "User not found");
            }
        } else if ("REVIEW".equals(entityType)) {
            // Nếu entity là review
            Optional<Review> reviewOptional = reviewRepository.findById(entityId);
            if (reviewOptional.isPresent()) {
                Review review = reviewOptional.get();
                // Cập nhật ảnh đại diện cho review nếu chưa có
                if (review.getImageUrl() == null || review.getImageUrl().isEmpty()) {
                    review.setImageUrl(imageUrl);
                    reviewRepository.save(review);
                }
                
                Image image1 = Image.builder()
                        .imageUrl(imageUrl)
                        .entityType(entityType)
                        .entityId(entityId)
                        .build();
                image = imageRepository.save(image1);
            } else {
                throw new VchefApiException(HttpStatus.NOT_FOUND, "Review not found");
            }
        }
        return image.getImageUrl();
    }
    
    public List<Image> getImagesByEntity(String entityType, Long entityId) {
        return imageRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }
}
