package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailDto;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailRequestDto;
import com.spring2025.vietchefs.models.payload.dto.DishDto;
import com.spring2025.vietchefs.models.payload.responseModel.BookingDetailsResponse;
import com.spring2025.vietchefs.models.payload.responseModel.DishesResponse;
import com.spring2025.vietchefs.models.payload.responseModel.TimeTravelResponse;
import com.spring2025.vietchefs.repositories.BookingDetailItemRepository;
import com.spring2025.vietchefs.repositories.BookingDetailRepository;
import com.spring2025.vietchefs.repositories.BookingRepository;
import com.spring2025.vietchefs.repositories.DishRepository;
import com.spring2025.vietchefs.services.BookingDetailService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class BookingDetailServiceImpl implements BookingDetailService {
    @Autowired
    private BookingDetailRepository bookingDetailRepository;
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingDetailItemRepository bookingDetailItemRepository;

    @Autowired
    private DishRepository dishRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private CalculateService calculateService;
    @Override
    public BookingDetail createBookingDetail(Booking booking, BookingDetailRequestDto dto) {
        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);
        detail.setSessionDate(dto.getSessionDate());
        detail.setStartTime(dto.getStartTime());
        detail.setEndTime(dto.getEndTime());
        detail.setLocation(dto.getLocation());
        detail.setIsServing(dto.getIsServing());
        detail.setIsDeleted(false);
        detail.setTimeBeginCook(dto.getTimeBeginCook());
        detail.setTimeBeginTravel(dto.getTimeBeginTravel());
        List<BookingDetailItem> dishes = dto.getDishes().stream()
                .map(itemDto -> {
                    Dish dish = dishRepository.findById(itemDto.getDishId())
                            .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found"));
                    BookingDetailItem detailDish = new BookingDetailItem();
                    detailDish.setBookingDetail(detail);
                    detailDish.setDish(dish);
                    detailDish.setNotes(itemDto.getNotes());
                    return detailDish;
                }).collect(Collectors.toList());
        detail.setDishes(dishes);
        detail.setTotalPrice(dto.getTotalPrice());

        return bookingDetailRepository.save(detail);
    }

    @Override
    public BookingDetailsResponse getBookingDetailByBooking(Long bookingId, int pageNo, int pageSize, String sortBy, String sortDir) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Booking not found with id: "+ bookingId));
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<BookingDetail> bookingDetails = bookingDetailRepository.findByBookingAndIsDeletedFalse(booking,pageable);

        // get content for page object
        List<BookingDetail> listOfBds = bookingDetails.getContent();

        List<BookingDetailDto> content = listOfBds.stream().map(bt -> modelMapper.map(bt, BookingDetailDto.class)).collect(Collectors.toList());

        BookingDetailsResponse templatesResponse = new BookingDetailsResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(bookingDetails.getNumber());
        templatesResponse.setPageSize(bookingDetails.getSize());
        templatesResponse.setTotalElements(bookingDetails.getTotalElements());
        templatesResponse.setTotalPages(bookingDetails.getTotalPages());
        templatesResponse.setLast(bookingDetails.isLast());
        return templatesResponse;
    }
}
