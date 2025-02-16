package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.dto.ChefDto;

public interface ChefService {
    ChefDto createChef (ChefDto chefDto);
    ChefDto getChefById(Long id);

}
