package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.payload.dto.ChefDto;
import com.spring2025.vietchefs.models.payload.requestModel.ChefRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefsResponse;

import java.util.List;

public interface ChefService {
    ChefDto createChef (ChefDto chefDto);
    void updateReputation(Chef chef, int delta);
    ChefResponseDto getChefById(Long id);
    ChefResponseDto registerChefRequest(Long userId, ChefRequestDto requestDto); // Đăng ký làm đầu bếp (Chờ duyệt)
    ChefResponseDto approveChef(Long chefId); // Admin duyệt Chef
    ChefsResponse getAllChefs(int pageNo, int pageSize, String sortBy, String sortDir); // Lấy danh sách tất cả đầu bếp
    ChefsResponse getAllChefsNearBy( double customerLat, double customerLng, double distance,int pageNo, int pageSize, String sortBy, String sortDir);
    ChefResponseDto updateChef(Long chefId, ChefRequestDto requestDto); // Cập nhật thông tin đầu bếp
    void deleteChef(Long chefId); // Xóa đầu bếp (Chỉ Admin)


}
