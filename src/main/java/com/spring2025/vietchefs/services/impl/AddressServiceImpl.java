package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Address;
import com.spring2025.vietchefs.models.entity.User;
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
    private ModelMapper modelMapper;

    @Override
    public AddressResponse getAddressById(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Address not found with id:" + id));
        return modelMapper.map(address, AddressResponse.class);
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
        Long userId = SecurityUtils.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));

        List<Address> existingAddresses = addressRepository.findByUserAndIsDeletedFalse(user);
        if (existingAddresses.size() >= 5) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "User cannot have more than 5 addresses");
        }

        Address address = modelMapper.map(request, Address.class);

        address.setUser(user);

        Address savedAddress = addressRepository.save(address);
        return modelMapper.map(savedAddress, AddressResponse.class);
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
