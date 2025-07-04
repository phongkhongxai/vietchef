package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.DishDto;
import com.spring2025.vietchefs.models.payload.requestModel.DishRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.DishResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.DishesResponse;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.DishService;
import com.spring2025.vietchefs.services.ReviewService;
import com.spring2025.vietchefs.utils.AppConstants;
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
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    private MenuItemRepository menuItemRepository;
    @Autowired
    private ChefRepository chefRepository;
    @Autowired
    private FoodTypeRepository foodTypeRepository;
    @Autowired
    private CalculateService calculateService;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ImageService imageService;
    @Override
    @Transactional
    public DishDto createDish(DishDto dishDto, MultipartFile imageFile) {
        Chef chef = chefRepository.findById(dishDto.getChefId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Chef not found."));
        List<FoodType> foodTypes = foodTypeRepository.findAllByIdInAndIsDeletedFalse(dishDto.getFoodTypeIds());
        if (foodTypes.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "No valid FoodTypes found for the given IDs.");
        }
        Dish dish = modelMapper.map(dishDto, Dish.class);
        dish.setFoodTypes(foodTypes);
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
    public DishResponseDto getDishById(Long id) {
        Optional<Dish> dish = dishRepository.findById(id);
        if (dish.isEmpty()){
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found with id: "+ id);
        }
        return modelMapper.map(dish.get(), DishResponseDto.class);

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
        if (dishRequest.getFoodTypeIds() != null && !dishRequest.getFoodTypeIds().isEmpty()) {
            List<FoodType> foodTypes = foodTypeRepository.findAllByIdInAndIsDeletedFalse(dishRequest.getFoodTypeIds());
            if (foodTypes.isEmpty()) {
                throw new VchefApiException(HttpStatus.NOT_FOUND, "No valid FoodTypes found for the given IDs.");
            }

            dish.setFoodTypes(foodTypes);
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
        if (dishOptional.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found with id: " + id);
        }
        Dish dish = dishOptional.get();
        List<MenuItem> relatedMenuItems = menuItemRepository.findAllByDish(dish);
        Set<Menu> relatedMenus = relatedMenuItems.stream()
                .map(MenuItem::getMenu)
                .collect(Collectors.toSet());
        for (Menu menu : relatedMenus) {
            menu.setIsDeleted(true);
            menuRepository.save(menu);
        }
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
        Sort sort;
        // Xử lý trường hợp sắp xếp theo đánh giá
        if (sortBy.equals("rating") || sortBy.equals("distance")|| AppConstants.DEFAULT_SORT_RATING_DESC.equals(sortDir)) {
            sort = Sort.by("id").ascending();
        } else {
            sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
        }

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Dish> dishesPage = dishRepository.findDishesNearCustomer(customerLat, customerLng, distance, pageable);
        List<Dish> dishes = dishesPage.getContent();

        List<DishResponseDto> content = dishes.stream()
                .map(dish -> {
                    DishResponseDto dto = modelMapper.map(dish, DishResponseDto.class);
                    Chef chef = dish.getChef();
                    if (chef != null && chef.getLatitude() != null && chef.getLongitude() != null) {
                        double dist = calculateService.calculateDistance(customerLat, customerLng, chef.getLatitude(), chef.getLongitude());
                        if (dto.getChef() != null) {
                            dto.getChef().setDistance(dist);
                            dto.getChef().setAverageRating(reviewService.getAverageRatingForChef(chef.getId()));
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());
        if (sortBy.equals("rating")) {
            content.sort(sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                    Comparator.comparing(dish -> dish.getChef().getAverageRating(), Comparator.<BigDecimal>nullsLast(Comparator.naturalOrder())) :
                    Comparator.comparing(dish -> dish.getChef().getAverageRating(), Comparator.<BigDecimal>nullsLast(Comparator.reverseOrder()))
            );
        } else if (sortBy.equals("distance")) {
            content.sort(sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                    Comparator.comparing(dish -> dish.getChef().getDistance(), Comparator.<Double>nullsLast(Comparator.naturalOrder())) :
                    Comparator.comparing(dish -> dish.getChef().getDistance(), Comparator.<Double>nullsLast(Comparator.reverseOrder()))
            );
        } else if (AppConstants.DEFAULT_SORT_RATING_DESC.equals(sortDir)) {
            content.sort(Comparator.comparing(dish -> dish.getChef().getAverageRating(), Comparator.nullsLast(Comparator.reverseOrder())));
        }

        DishesResponse response = new DishesResponse();
        response.setContent(content);
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        response.setTotalElements((int) dishesPage.getTotalElements());
        response.setTotalPages(dishesPage.getTotalPages());
        response.setLast(dishesPage.isLast());

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
    public DishesResponse getDishesByFoodType(List<Long> foodTypeIds, int pageNo, int pageSize, String sortBy, String sortDir) {
        List<FoodType> foodTypes = foodTypeRepository.findAllById(foodTypeIds);
        if (foodTypes.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Không tìm thấy loại món ăn phù hợp với các ID: " + foodTypeIds);
        }
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Dish> dishes = dishRepository.findDistinctByFoodTypesInAndIsDeletedFalse(foodTypes,pageable);
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
    public DishesResponse getDishesByFoodTypeNearBy(List<Long> foodTypeIds, double customerLat, double customerLng,double distance, int pageNo, int pageSize, String sortBy, String sortDir) {
        List<FoodType> foodTypes = foodTypeRepository.findAllById(foodTypeIds);
        if (foodTypes.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Không tìm thấy loại món ăn phù hợp với các ID: " + foodTypeIds);
        }
        Sort sort;
        // Xử lý trường hợp sắp xếp theo đánh giá
        if (sortBy.equals("rating") || sortBy.equals("distance")|| AppConstants.DEFAULT_SORT_RATING_DESC.equals(sortDir)) {
            sort = Sort.by("id").ascending();
        } else {
            sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
        }

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Dish> dishes = dishRepository.findDishesByFoodTypesAndDistance(foodTypeIds,customerLat,customerLng,distance,pageable);
        // get content for page object
        List<Dish> allDishes = dishes.getContent();
        List<DishResponseDto> content = allDishes.stream()
                .map(dish -> {
                    DishResponseDto dto = modelMapper.map(dish, DishResponseDto.class);
                    Chef chef = dish.getChef();
                    if (chef != null && chef.getLatitude() != null && chef.getLongitude() != null) {
                        double dist = calculateService.calculateDistance(customerLat, customerLng, chef.getLatitude(), chef.getLongitude());
                        if (dto.getChef() != null) {
                            dto.getChef().setDistance(dist);
                            dto.getChef().setAverageRating(reviewService.getAverageRatingForChef(chef.getId()));
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());
        if (sortBy.equals("rating")) {
            content.sort(sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                    Comparator.comparing(dish -> dish.getChef().getAverageRating(), Comparator.<BigDecimal>nullsLast(Comparator.naturalOrder())) :
                    Comparator.comparing(dish -> dish.getChef().getAverageRating(), Comparator.<BigDecimal>nullsLast(Comparator.reverseOrder()))
            );
        } else if (sortBy.equals("distance")) {
            content.sort(sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                    Comparator.comparing(dish -> dish.getChef().getDistance(), Comparator.<Double>nullsLast(Comparator.naturalOrder())) :
                    Comparator.comparing(dish -> dish.getChef().getDistance(), Comparator.<Double>nullsLast(Comparator.reverseOrder()))
            );
        } else if (AppConstants.DEFAULT_SORT_RATING_DESC.equals(sortDir)) {
            content.sort(Comparator.comparing(dish -> dish.getChef().getAverageRating(), Comparator.nullsLast(Comparator.reverseOrder())));
        }
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
        Sort sort;
        // Xử lý trường hợp sắp xếp theo đánh giá
        if (sortBy.equals("rating") || sortBy.equals("distance")|| AppConstants.DEFAULT_SORT_RATING_DESC.equals(sortDir)) {
            sort = Sort.by("id").ascending();
        } else {
            sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
        }

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Dish> dishesPage = dishRepository.searchDishesByKeywordAndDistance(keyword,customerLat,customerLng,distance,pageable);
        List<Dish> allDishes = dishesPage.getContent();
        List<DishResponseDto> content = allDishes.stream()
                .map(dish -> {
                    DishResponseDto dto = modelMapper.map(dish, DishResponseDto.class);
                    Chef chef = dish.getChef();
                    if (chef != null && chef.getLatitude() != null && chef.getLongitude() != null) {
                        double dist = calculateService.calculateDistance(customerLat, customerLng, chef.getLatitude(), chef.getLongitude());
                        if (dto.getChef() != null) {
                            dto.getChef().setDistance(dist);
                            dto.getChef().setAverageRating(reviewService.getAverageRatingForChef(chef.getId()));
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());
        if (sortBy.equals("rating")) {
            content.sort(sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                    Comparator.comparing(dish -> dish.getChef().getAverageRating(), Comparator.<BigDecimal>nullsLast(Comparator.naturalOrder())) :
                    Comparator.comparing(dish -> dish.getChef().getAverageRating(), Comparator.<BigDecimal>nullsLast(Comparator.reverseOrder()))
            );
        } else if (sortBy.equals("distance")) {
            content.sort(sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                    Comparator.comparing(dish -> dish.getChef().getDistance(), Comparator.<Double>nullsLast(Comparator.naturalOrder())) :
                    Comparator.comparing(dish -> dish.getChef().getDistance(), Comparator.<Double>nullsLast(Comparator.reverseOrder()))
            );
        } else if (AppConstants.DEFAULT_SORT_RATING_DESC.equals(sortDir)) {
            content.sort(Comparator.comparing(dish -> dish.getChef().getAverageRating(), Comparator.nullsLast(Comparator.reverseOrder())));
        }
        DishesResponse templatesResponse = new DishesResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(dishesPage.getNumber());
        templatesResponse.setPageSize(dishesPage.getSize());
        templatesResponse.setTotalElements(dishesPage.getTotalElements());
        templatesResponse.setTotalPages(dishesPage.getTotalPages());
        templatesResponse.setLast(dishesPage.isLast());

        return templatesResponse;
    }
}
