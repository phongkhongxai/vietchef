package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.ChefDto;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.ChefService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;
@Service
public class ChefServiceImpl implements ChefService {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ChefRepository chefRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public ChefDto createChef(ChefDto chefDto) {
        User user = userRepository.findById(chefDto.getUserId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"User not found"));

        // Kiểm tra role của User, chỉ cho phép nếu role là "chef"
        if (!"ROLE_CHEF".equalsIgnoreCase(user.getRole().getRoleName())) {
            throw new VchefApiException(HttpStatus.UNAUTHORIZED,"User does not have chef role.");
        }

        // Kiểm tra xem User đã có hồ sơ Chef chưa
        Optional<Chef> existingChef = chefRepository.findByUser(user);
        if(existingChef.isPresent()){
            throw new VchefApiException(HttpStatus.BAD_REQUEST,"Chef profile already exists for this user.");
        }

        Chef chef = new Chef();
        chef.setUser(user);
        chef.setBio(chefDto.getBio());
        chef.setDescription(chefDto.getDescription());
        chef.setPrice(chefDto.getPrice());
        chef.setStatus(chefDto.getStatus() != null ? chefDto.getStatus() : "active");
        chef = chefRepository.save(chef);

        return modelMapper.map(chefRepository.save(chef), ChefDto.class);
    }

    @Override
    public ChefDto getChefById(Long id) {
        Optional<Chef> chef = chefRepository.findById(id);
        if(chef.isEmpty()){
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with id: "+ id);

        }
        return modelMapper.map(chef.get(), ChefDto.class);
    }
}
