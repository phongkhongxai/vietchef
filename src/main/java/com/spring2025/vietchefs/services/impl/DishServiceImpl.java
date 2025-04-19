package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.Dish;
import com.spring2025.vietchefs.models.entity.FoodType;
import com.spring2025.vietchefs.models.entity.Menu;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.DishDto;
import com.spring2025.vietchefs.models.payload.requestModel.DishRequest;
import com.spring2025.vietchefs.models.payload.responseModel.DishResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.DishesResponse;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.DishRepository;
import com.spring2025.vietchefs.repositories.FoodTypeRepository;
import com.spring2025.vietchefs.repositories.MenuRepository;
import com.spring2025.vietchefs.services.DishService;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private DishRepository dishRepository;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private ChefRepository chefRepository;
    @Autowired
    private FoodTypeRepository foodTypeRepository;
    @Autowired
    private CalculateService calculateService;
    @Autowired
    private ImageService imageService;
    @Override
    @Transactional
    public DishDto createDish(DishDto dishDto, MultipartFile imageFile) {
        Chef chef = chefRepository.findById(dishDto.getChefId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Chef not found."));
        FoodType foodType = foodTypeRepository.findById(dishDto.getFoodTypeId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"FoodType not found."));
        Dish dish = modelMapper.map(dishDto, Dish.class);
        dish = dishRepository.save(dish);
        if (imageFile != null) {
            try {
                String imageUrl = imageService.uploadImage(imageFile,dish.getId(), "DISH");
                dish.setImageUrl(imageUrl);
            } catch (IOException e) {
                throw new VchefApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload image.");
            }
        }
        return modelMapper.map(dish, DishDto.class);
    }

    @Override
    public DishDto getDishById(Long id) {
        Optional<Dish> dish = dishRepository.findById(id);
        if (dish.isEmpty()){
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found with id: "+ id);
        }
        return modelMapper.map(dish.get(), DishDto.class);

    }

    @Override
    public DishDto updateDish(Long id, DishRequest dishRequest) {
        Optional<Dish> dishOptional = dishRepository.findById(id);
        if (dishOptional.isEmpty()){
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found with id: "+ id);
        }
        Dish dish = dishOptional.get();
        if (dishRequest.getChefId() != null) {
            Chef chef = chefRepository.findById(dishRequest.getChefId())
                    .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Chef not found with id " + dishRequest.getChefId()));
            dish.setChef(chef);
        }
        if (dishRequest.getFoodTypeId() != null) {
            FoodType foodType = foodTypeRepository.findById(dishRequest.getFoodTypeId())
                    .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"FoodType not found with id " + dishRequest.getFoodTypeId()));
            dish.setFoodType(foodType);
        }
        dish.setName(dishRequest.getName() != null ? dishRequest.getName() : dish.getName());
        dish.setDescription(dishRequest.getDescription() != null ? dishRequest.getDescription() : dish.getDescription());
        dish.setCuisineType(dishRequest.getCuisineType() != null ? dishRequest.getCuisineType() : dish.getCuisineType());
        dish.setServiceType(dishRequest.getServiceType() != null ? dishRequest.getServiceType() : dish.getServiceType());
        dish.setCookTime(dishRequest.getCookTime() != null ? dishRequest.getCookTime() : dish.getCookTime());
        dish.setBasePrice(dishRequest.getBasePrice() != null ? dishRequest.getBasePrice() : dish.getBasePrice());
        dish.setEstimatedCookGroup(dishRequest.getEstimatedCookGroup()!=null ? dishRequest.getEstimatedCookGroup() : dish.getEstimatedCookGroup());
        Dish updatedDish = dishRepository.save(dish);
        if (dishRequest.getFile() != null && !dishRequest.getFile().isEmpty()) {
            try{
                String imageUrl = imageService.uploadImage(dishRequest.getFile(), id, "DISH");
                updatedDish.setImageUrl(imageUrl);
            } catch (IOException e) {
                throw new VchefApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload image.");
            }
        }
        return modelMapper.map(updatedDish, DishDto.class);
    }

    @Override
    public String deleteDish(Long id) {
        Optional<Dish> dishOptional = dishRepository.findById(id);
        if (dishOptional.isEmpty()){
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found with id: "+ id);
        }
        Dish dish = dishOptional.get();
        dish.setIsDeleted(true);
        dishRepository.save(dish);
        return "Deleted dish successfully";
    }

    @Override
    public DishesResponse getAllDishes(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Dish> dishes = dishRepository.findAllNotDeleted(pageable);

        // get content for page object
        List<Dish> listOfDishes = dishes.getContent();

        List<DishResponseDto> content = listOfDishes.stream().map(bt -> modelMapper.map(bt, DishResponseDto.class)).collect(Collectors.toList());

        DishesResponse templatesResponse = new DishesResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(dishes.getNumber());
        templatesResponse.setPageSize(dishes.getSize());
        templatesResponse.setTotalElements(dishes.getTotalElements());
        templatesResponse.setTotalPages(dishes.getTotalPages());
        templatesResponse.setLast(dishes.isLast());
        return templatesResponse;
    }

    @Override
    public DishesResponse getDishesNearBy(double customerLat, double customerLng, double distance, int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Dish> dishesPage = dishRepository.findAllNotDeleted(pageable);
        List<Dish> allDishes = dishesPage.getContent();

        // Lọc các món có chef ở gần khách hàng
        List<Dish> filteredDishes = allDishes.stream()
                .filter(dish -> {
                    Chef chef = dish.getChef();
                    if (chef == null || chef.getLatitude() == null || chef.getLongitude() == null) return false;
                    double chefLat = chef.getLatitude();
                    double chefLng = chef.getLongitude();
                    double distanceToCustomer = calculateService.calculateDistance(customerLat, customerLng, chefLat, chefLng);
                    return distanceToCustomer <= distance;
                })
                .toList();

        // Chuyển sang DTO
        List<DishResponseDto> content = filteredDishes.stream()
                .map(dish -> modelMapper.map(dish, DishResponseDto.class))
                .collect(Collectors.toList());

        DishesResponse response = new DishesResponse();
        response.setContent(content);
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        response.setTotalElements(filteredDishes.size());
        response.setTotalPages((int) Math.ceil((double) filteredDishes.size() / pageSize));
        response.setLast(filteredDishes.size() <= (pageNo + 1) * pageSize);

        return response;
    }

    @Override
    public DishesResponse getDishesByChef(Long chefId, int pageNo, int pageSize, String sortBy, String sortDir) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Chef not found with id: "+ chefId));
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Dish> dishes = dishRepository.findByChefAndIsDeletedFalse(chef,pageable);

        // get content for page object
        List<Dish> listOfDishes = dishes.getContent();

        List<DishResponseDto> content = listOfDishes.stream().map(bt -> modelMapper.map(bt, DishResponseDto.class)).collect(Collectors.toList());

        DishesResponse templatesResponse = new DishesResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(dishes.getNumber());
        templatesResponse.setPageSize(dishes.getSize());
        templatesResponse.setTotalElements(dishes.getTotalElements());
        templatesResponse.setTotalPages(dishes.getTotalPages());
        templatesResponse.setLast(dishes.isLast());
        return templatesResponse;
    }

    @Override
    public DishesResponse getDishesNotInMenu(Long menuId, int pageNo, int pageSize, String sortBy, String sortDir) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Menu not found with id: "+ menuId));
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Dish> dishes = dishRepository.findByNotInMenuAndIsDeletedFalseAndChefId(menu.getId(), menu.getChef().getId(),pageable);

        // get content for page object
        List<Dish> listOfDishes = dishes.getContent();

        List<DishResponseDto> content = listOfDishes.stream().map(bt -> modelMapper.map(bt, DishResponseDto.class)).collect(Collectors.toList());

        DishesResponse templatesResponse = new DishesResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(dishes.getNumber());
        templatesResponse.setPageSize(dishes.getSize());
        templatesResponse.setTotalElements(dishes.getTotalElements());
        templatesResponse.setTotalPages(dishes.getTotalPages());
        templatesResponse.setLast(dishes.isLast());
        return templatesResponse;
    }

    @Override
    public DishesResponse getDishesByFoodType(Long foodTypeId, int pageNo, int pageSize, String sortBy, String sortDir) {
        FoodType foodType = foodTypeRepository.findById(foodTypeId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"FoodType not found with id: "+ foodTypeId));
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Dish> dishes = dishRepository.findByFoodTypeAndIsDeletedFalse(foodType,pageable);

        // get content for page object
        List<Dish> listOfDishes = dishes.getContent();

        List<DishResponseDto> content = listOfDishes.stream().map(bt -> modelMapper.map(bt, DishResponseDto.class)).collect(Collectors.toList());

        DishesResponse templatesResponse = new DishesResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(dishes.getNumber());
        templatesResponse.setPageSize(dishes.getSize());
        templatesResponse.setTotalElements(dishes.getTotalElements());
        templatesResponse.setTotalPages(dishes.getTotalPages());
        templatesResponse.setLast(dishes.isLast());
        return templatesResponse;
    }

    @Override
    public DishesResponse searchDishByName(String keyword, int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Dish> dishes = dishRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndIsDeletedFalse(keyword,keyword,pageable);

        // get content for page object
        List<Dish> listOfDishes = dishes.getContent();

        List<DishResponseDto> content = listOfDishes.stream().map(bt -> modelMapper.map(bt, DishResponseDto.class)).collect(Collectors.toList());

        DishesResponse templatesResponse = new DishesResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(dishes.getNumber());
        templatesResponse.setPageSize(dishes.getSize());
        templatesResponse.setTotalElements(dishes.getTotalElements());
        templatesResponse.setTotalPages(dishes.getTotalPages());
        templatesResponse.setLast(dishes.isLast());
        return templatesResponse;
    }

    @Override
    public DishesResponse searchDishByNameNearBy(double customerLat, double customerLng, double distance, String keyword, int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Dish> dishesPage = dishRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndIsDeletedFalse(keyword,keyword,pageable);
        List<Dish> allDishes = dishesPage.getContent();

        // Lọc các món có chef ở gần khách hàng
        List<Dish> filteredDishes = allDishes.stream()
                .filter(dish -> {
                    Chef chef = dish.getChef();
                    if (chef == null || chef.getLatitude() == null || chef.getLongitude() == null) return false;
                    double chefLat = chef.getLatitude();
                    double chefLng = chef.getLongitude();
                    double distanceToCustomer = calculateService.calculateDistance(customerLat, customerLng, chefLat, chefLng);
                    return distanceToCustomer <= distance;
                })
                .toList();

        // Chuyển sang DTO
        List<DishResponseDto> content = filteredDishes.stream()
                .map(dish -> modelMapper.map(dish, DishResponseDto.class))
                .collect(Collectors.toList());

        DishesResponse response = new DishesResponse();
        response.setContent(content);
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        response.setTotalElements(filteredDishes.size());
        response.setTotalPages((int) Math.ceil((double) filteredDishes.size() / pageSize));
        response.setLast(filteredDishes.size() <= (pageNo + 1) * pageSize);

        return response;
    }
}
