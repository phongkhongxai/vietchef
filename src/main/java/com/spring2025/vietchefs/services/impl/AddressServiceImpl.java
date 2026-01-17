package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Address;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.CreateAddressRequest;
import com.spring2025.vietchefs.models.payload.requestModel.UpdateAddressRequest;
import com.spring2025.vietchefs.models.payload.responseModel.AddressResponse;
import com.spring2025.vietchefs.repositories.AddressRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.security.JwtTokenProvider;
import com.spring2025.vietchefs.services.AddressService;
import com.spring2025.vietchefs.utils.SecurityUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private DistanceService distanceService;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public AddressResponse getAddressById(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address","id",id));
        return modelMapper.map(address, AddressResponse.class);
    }

    @Override
    public List<AddressResponse> getMyAddress() {
        // 1. Lấy đối tượng Authentication từ context
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. Trích xuất userId từ claim (ép kiểu về Jwt)
        Long userId;
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            userId = jwt.getClaim("userId");
        } else {
            userId = null;
        }

        if (userId == null) {
            throw new VchefApiException(HttpStatus.FORBIDDEN,"Không tìm thấy UserId trong Token");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));
        List<Address> addresses = addressRepository.findByUserAndIsDeletedFalse(user);

        return addresses.stream()
                .map(address -> modelMapper.map(address, AddressResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<AddressResponse> getAddressesFromUser() {
        Long userId = SecurityUtils.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));

        List<Address> addresses = addressRepository.findByUserAndIsDeletedFalse(user);

        return addresses.stream()
                .map(address -> modelMapper.map(address, AddressResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public AddressResponse createAddress(CreateAddressRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. Trích xuất userId từ claim (ép kiểu về Jwt)
        Long userId;
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            userId = jwt.getClaim("userId");
        } else {
            userId = null;
        }

        if (userId == null) {
            throw new VchefApiException(HttpStatus.FORBIDDEN,"Không tìm thấy UserId trong Token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));

        List<Address> existingAddresses = addressRepository.findByUserAndIsDeletedFalse(user);
        if (existingAddresses.size() >= 5) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "User cannot have more than 5 addresses");
        }

        Address address = modelMapper.map(request, Address.class);

        address.setUser(user);
        double[] latLng = getLatLngFromAddress(address.getAddress());
        address.setLatitude(latLng[0]);
        address.setLongitude(latLng[1]);
        Address savedAddress = addressRepository.save(address);
        return modelMapper.map(savedAddress, AddressResponse.class);
    }
    public double[] getLatLngFromAddress(String address) {
        // API hết hạn - Trả về tọa độ ngẫu nhiên
        double lat = -90.0 + (Math.random() * 180.0);
        double lng = -180.0 + (Math.random() * 360.0);

        return new double[]{lat, lng};
    }

    @Override
    public AddressResponse updateAddress(UpdateAddressRequest request) {
        Long addressId = request.getId();
        Address existingAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Address not found with id: " + addressId));

        if (request.getTitle() != null) {
            existingAddress.setTitle(request.getTitle());
        }

        if (request.getAddress() != null) {
            existingAddress.setAddress(request.getAddress());
            double[] latLng = distanceService.getLatLngFromAddress(request.getAddress());
            existingAddress.setLatitude(latLng[0]);
            existingAddress.setLongitude(latLng[1]);
        }
        Address updatedAddress = addressRepository.save(existingAddress);
        return modelMapper.map(updatedAddress, AddressResponse.class);
    }

    @Override
    public void deleteAddress(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Address not found with id: " + id));
        address.setDeleted(true);
        addressRepository.save(address);
    }

}
