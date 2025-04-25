package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.dto.FavoriteChefDto;
import com.spring2025.vietchefs.models.payload.responseModel.FavoriteChefsResponse;

public interface FavoriteChefService {
    // Thêm đầu bếp vào danh sách yêu thích
    FavoriteChefDto addFavoriteChef(Long userId, Long chefId);
    
    // Xóa đầu bếp khỏi danh sách yêu thích
    void removeFavoriteChef(Long userId, Long chefId);
    
    // Lấy danh sách đầu bếp yêu thích của một user
    FavoriteChefsResponse getFavoriteChefs(Long userId, int pageNo, int pageSize, String sortBy, String sortDir);
    
    // Kiểm tra một đầu bếp có nằm trong danh sách yêu thích của user không
    boolean isChefFavorite(Long userId, Long chefId);
} 