package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.FoodType;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.FoodTypeDto;
import com.spring2025.vietchefs.repositories.FoodTypeRepository;
import com.spring2025.vietchefs.services.FoodTypeService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FoodTypeServiceImpl implements FoodTypeService {
    @Autowired
    private FoodTypeRepository foodTypeRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Override
    public FoodTypeDto createFoodType(FoodTypeDto foodTypeDto) {
        FoodType existingType = foodTypeRepository.findByName(foodTypeDto.getName());
        if (existingType != null) {
            throw new RuntimeException("Food type already exists");
        }

        FoodType foodType = modelMapper.map(foodTypeDto, FoodType.class);
        return modelMapper.map(foodTypeRepository.save(foodType), FoodTypeDto.class);
    }

    @Override
    public FoodTypeDto updateType(Long id, FoodTypeDto foodTypeDto) {
        Optional<FoodType> foodTypeOptional = foodTypeRepository.findById(id);
        if (foodTypeOptional.isEmpty()){
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found with id: "+ id);
        }
        FoodType foodType = foodTypeOptional.get();
        foodType.setName(foodTypeDto.getName() != null ? foodTypeDto.getName() : foodType.getName());
        FoodType foodType1 = foodTypeRepository.save(foodType);
        return modelMapper.map(foodType1, FoodTypeDto.class);
    }

    @Override
    public String deleteType(Long id) {
        Optional<FoodType> foodTypeOptional = foodTypeRepository.findById(id);
        if (foodTypeOptional.isEmpty()){
            throw new VchefApiException(HttpStatus.NOT_FOUND, "FoodType not found with id: "+ id);
        }
        FoodType foodType = foodTypeOptional.get();
        foodType.setIsDeleted(true);
        foodTypeRepository.save(foodType);
        return "Deleted FoodType successfully";
    }

    @Override
    public List<FoodTypeDto> getAllFoodTypes() {
        List<FoodType> foodTypes = foodTypeRepository.findAllNotDeleted();
        return foodTypes.stream().map(bt -> modelMapper.map(bt, FoodTypeDto.class)).collect(Collectors.toList());
    }

    @Override
    public FoodTypeDto getById(Long id) {
        Optional<FoodType> foodType = foodTypeRepository.findById(id);
        if (foodType.isEmpty()){
            throw new VchefApiException(HttpStatus.NOT_FOUND, "FoodType not found with id: "+ id);
        }
        return modelMapper.map(foodType.get(), FoodTypeDto.class);
    }
}
