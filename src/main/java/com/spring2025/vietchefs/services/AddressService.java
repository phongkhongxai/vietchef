package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.requestModel.CreateAddressRequest;
import com.spring2025.vietchefs.models.payload.requestModel.UpdateAddressRequest;
import com.spring2025.vietchefs.models.payload.responseModel.AddressResponse;

import java.util.List;

public interface AddressService {
    AddressResponse getAddressById(Long id);
    List<AddressResponse> getAddressesFromUser();
    AddressResponse createAddress(CreateAddressRequest request);
    AddressResponse updateAddress(UpdateAddressRequest request);
    void deleteAddress(Long id);
}
