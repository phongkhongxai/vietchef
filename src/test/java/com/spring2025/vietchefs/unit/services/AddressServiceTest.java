package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.Address;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.CreateAddressRequest;
import com.spring2025.vietchefs.models.payload.requestModel.UpdateAddressRequest;
import com.spring2025.vietchefs.models.payload.responseModel.AddressResponse;
import com.spring2025.vietchefs.repositories.AddressRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.security.JwtTokenProvider;
import com.spring2025.vietchefs.services.impl.AddressServiceImpl;
import com.spring2025.vietchefs.services.impl.DistanceService;
import com.spring2025.vietchefs.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private DistanceService distanceService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AddressServiceImpl addressService;
    
    @Captor
    private ArgumentCaptor<Address> addressCaptor;

    private Address testAddress;
    private User testUser;
    private AddressResponse addressResponse;
    private CreateAddressRequest createRequest;
    private UpdateAddressRequest updateRequest;
    private Long testUserId = 1L;

    @BeforeEach
    void setUp() {
        // Initialize test objects
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");

        testAddress = new Address();
        testAddress.setId(1L);
        testAddress.setUser(testUser);
        testAddress.setTitle("Home");
        testAddress.setAddress("123 Main St, City, Country");
        testAddress.setLatitude(10.0);
        testAddress.setLongitude(20.0);
        testAddress.setDeleted(false);

        addressResponse = new AddressResponse();
        addressResponse.setId(1L);
        addressResponse.setTitle("Home");
        addressResponse.setAddress("123 Main St, City, Country");
        addressResponse.setLatitude(10.0);
        addressResponse.setLongitude(20.0);

        createRequest = new CreateAddressRequest();
        createRequest.setTitle("Work");
        createRequest.setAddress("456 Office St, City, Country");

        updateRequest = new UpdateAddressRequest();
        updateRequest.setId(1L);
        updateRequest.setTitle("Updated Home");
        updateRequest.setAddress("789 Updated St, City, Country");
    }

    // ------------------------------------------------------------------------
    // Tests for getAddressById
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Test 1: getAddressById when address exists should return address response")
    void getAddressById_WhenAddressExists_ShouldReturnAddressResponse() {
        // Arrange
        when(addressRepository.findById(1L)).thenReturn(Optional.of(testAddress));
        when(modelMapper.map(testAddress, AddressResponse.class)).thenReturn(addressResponse);

        // Act
        AddressResponse result = addressService.getAddressById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Home", result.getTitle());
        assertEquals("123 Main St, City, Country", result.getAddress());
        assertEquals(10.0, result.getLatitude());
        assertEquals(20.0, result.getLongitude());
        verify(addressRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Test 2: getAddressById when address does not exist should throw VchefApiException")
    void getAddressById_WhenAddressDoesNotExist_ShouldThrowVchefApiException() {
        // Arrange
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class,
                () -> addressService.getAddressById(99L));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("Address not found with id:99"));
        verify(addressRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Test 3: getAddressById when address is deleted should still return address")
    void getAddressById_WhenAddressIsDeleted_ShouldStillReturnAddress() {
        // Arrange
        testAddress.setDeleted(true);
        when(addressRepository.findById(1L)).thenReturn(Optional.of(testAddress));
        when(modelMapper.map(testAddress, AddressResponse.class)).thenReturn(addressResponse);

        // Act
        AddressResponse result = addressService.getAddressById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Home", result.getTitle());
        verify(addressRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Test 4: getAddressById when repository throws exception should propagate exception")
    void getAddressById_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Arrange
        when(addressRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class,
                () -> addressService.getAddressById(1L));
        
        assertEquals("Database error", exception.getMessage());
        verify(addressRepository, times(1)).findById(1L);
    }

    // ------------------------------------------------------------------------
    // Tests for getAddressesFromUser
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Test 1: getAddressesFromUser when user has addresses should return list")
    void getAddressesFromUser_WhenUserHasAddresses_ShouldReturnList() {
        // Arrange
        List<Address> addresses = Collections.singletonList(testAddress);
        List<AddressResponse> addressResponses = Collections.singletonList(addressResponse);
        
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(addressRepository.findByUserAndIsDeletedFalse(testUser)).thenReturn(addresses);
            when(modelMapper.map(testAddress, AddressResponse.class)).thenReturn(addressResponse);
            
            // Act
            List<AddressResponse> results = addressService.getAddressesFromUser();
            
            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals("Home", results.get(0).getTitle());
            assertEquals("123 Main St, City, Country", results.get(0).getAddress());
            verify(userRepository, times(1)).findById(testUserId);
            verify(addressRepository, times(1)).findByUserAndIsDeletedFalse(testUser);
        }
    }

    @Test
    @DisplayName("Test 2: getAddressesFromUser when user does not exist should throw VchefApiException")
    void getAddressesFromUser_WhenUserDoesNotExist_ShouldThrowVchefApiException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(99L);
            
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class,
                    () -> addressService.getAddressesFromUser());
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertTrue(exception.getMessage().contains("User not found with id: 99"));
            verify(userRepository, times(1)).findById(99L);
            verify(addressRepository, never()).findByUserAndIsDeletedFalse(any());
        }
    }

    @Test
    @DisplayName("Test 3: getAddressesFromUser when user has no addresses should return empty list")
    void getAddressesFromUser_WhenUserHasNoAddresses_ShouldReturnEmptyList() {
        // Arrange
        List<Address> emptyList = new ArrayList<>();
        
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(addressRepository.findByUserAndIsDeletedFalse(testUser)).thenReturn(emptyList);
            
            // Act
            List<AddressResponse> results = addressService.getAddressesFromUser();
            
            // Assert
            assertNotNull(results);
            assertTrue(results.isEmpty());
            verify(userRepository, times(1)).findById(testUserId);
            verify(addressRepository, times(1)).findByUserAndIsDeletedFalse(testUser);
        }
    }

    @Test
    @DisplayName("Test 4: getAddressesFromUser when SecurityUtils throws exception should propagate exception")
    void getAddressesFromUser_WhenSecurityUtilsThrowsException_ShouldPropagateException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenThrow(new RuntimeException("Authentication error"));
            
            // Act & Assert
            Exception exception = assertThrows(RuntimeException.class,
                    () -> addressService.getAddressesFromUser());
            
            assertEquals("Authentication error", exception.getMessage());
            verify(userRepository, never()).findById(anyLong());
            verify(addressRepository, never()).findByUserAndIsDeletedFalse(any());
        }
    }

    // ------------------------------------------------------------------------
    // Tests for createAddress
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Test 1: createAddress with valid request should create and return address")
    void createAddress_WithValidRequest_ShouldCreateAndReturnAddress() {
        // Arrange
        Address newAddress = new Address();
        newAddress.setTitle("Work");
        newAddress.setAddress("456 Office St, City, Country");
        
        Address savedAddress = new Address();
        savedAddress.setId(2L);
        savedAddress.setUser(testUser);
        savedAddress.setTitle("Work");
        savedAddress.setAddress("456 Office St, City, Country");
        savedAddress.setLatitude(30.0);
        savedAddress.setLongitude(40.0);
        
        AddressResponse newAddressResponse = new AddressResponse();
        newAddressResponse.setId(2L);
        newAddressResponse.setTitle("Work");
        newAddressResponse.setAddress("456 Office St, City, Country");
        newAddressResponse.setLatitude(30.0);
        newAddressResponse.setLongitude(40.0);
        
        double[] latLng = {30.0, 40.0};
        
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(addressRepository.findByUserAndIsDeletedFalse(testUser)).thenReturn(new ArrayList<>());
            when(modelMapper.map(createRequest, Address.class)).thenReturn(newAddress);
            when(distanceService.getLatLngFromAddress(anyString())).thenReturn(latLng);
            when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);
            when(modelMapper.map(savedAddress, AddressResponse.class)).thenReturn(newAddressResponse);
            
            // Act
            AddressResponse result = addressService.createAddress(createRequest);
            
            // Assert
            assertNotNull(result);
            assertEquals(2L, result.getId());
            assertEquals("Work", result.getTitle());
            assertEquals("456 Office St, City, Country", result.getAddress());
            assertEquals(30.0, result.getLatitude());
            assertEquals(40.0, result.getLongitude());
            verify(addressRepository, times(1)).save(any(Address.class));
        }
    }

    @Test
    @DisplayName("Test 2: createAddress when user does not exist should throw VchefApiException")
    void createAddress_WhenUserDoesNotExist_ShouldThrowVchefApiException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(99L);
            
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class,
                    () -> addressService.createAddress(createRequest));
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertTrue(exception.getMessage().contains("User not found with id: 99"));
            verify(userRepository, times(1)).findById(99L);
            verify(addressRepository, never()).save(any(Address.class));
        }
    }

    @Test
    @DisplayName("Test 3: createAddress when user already has five addresses should throw VchefApiException")
    void createAddress_WhenUserAlreadyHasFiveAddresses_ShouldThrowVchefApiException() {
        // Arrange
        List<Address> fiveAddresses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Address address = new Address();
            address.setId((long) i + 1);
            address.setUser(testUser);
            address.setTitle("Address " + i);
            fiveAddresses.add(address);
        }
        
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(addressRepository.findByUserAndIsDeletedFalse(testUser)).thenReturn(fiveAddresses);
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class,
                    () -> addressService.createAddress(createRequest));
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("User cannot have more than 5 addresses"));
            verify(userRepository, times(1)).findById(testUserId);
            verify(addressRepository, times(1)).findByUserAndIsDeletedFalse(testUser);
            verify(addressRepository, never()).save(any(Address.class));
        }
    }

    @Test
    @DisplayName("Test 4: createAddress when geolocation fails should propagate exception")
    void createAddress_WhenGeolocationFails_ShouldPropagateException() {
        // Arrange
        Address newAddress = new Address();
        newAddress.setTitle("Work");
        newAddress.setAddress("456 Office St, City, Country");
        
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(addressRepository.findByUserAndIsDeletedFalse(testUser)).thenReturn(new ArrayList<>());
            when(modelMapper.map(createRequest, Address.class)).thenReturn(newAddress);
            when(distanceService.getLatLngFromAddress(anyString())).thenThrow(new RuntimeException("Geocoding error"));
            
            // Act & Assert
            Exception exception = assertThrows(RuntimeException.class,
                    () -> addressService.createAddress(createRequest));
            
            assertEquals("Geocoding error", exception.getMessage());
            verify(userRepository, times(1)).findById(testUserId);
            verify(distanceService, times(1)).getLatLngFromAddress(anyString());
            verify(addressRepository, never()).save(any(Address.class));
        }
    }

    // ------------------------------------------------------------------------
    // Tests for updateAddress
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Test 1: updateAddress with valid request should update and return address")
    void updateAddress_WithValidRequest_ShouldUpdateAndReturnAddress() {
        // Arrange
        Address existingAddress = new Address();
        existingAddress.setId(1L);
        existingAddress.setUser(testUser);
        existingAddress.setTitle("Home");
        existingAddress.setAddress("123 Main St, City, Country");
        existingAddress.setLatitude(10.0);
        existingAddress.setLongitude(20.0);
        
        Address updatedAddress = new Address();
        updatedAddress.setId(1L);
        updatedAddress.setUser(testUser);
        updatedAddress.setTitle("Updated Home");
        updatedAddress.setAddress("789 Updated St, City, Country");
        updatedAddress.setLatitude(50.0);
        updatedAddress.setLongitude(60.0);
        
        AddressResponse updatedResponse = new AddressResponse();
        updatedResponse.setId(1L);
        updatedResponse.setTitle("Updated Home");
        updatedResponse.setAddress("789 Updated St, City, Country");
        updatedResponse.setLatitude(50.0);
        updatedResponse.setLongitude(60.0);
        
        double[] latLng = {50.0, 60.0};
        
        when(addressRepository.findById(1L)).thenReturn(Optional.of(existingAddress));
        when(distanceService.getLatLngFromAddress(updateRequest.getAddress())).thenReturn(latLng);
        when(addressRepository.save(any(Address.class))).thenReturn(updatedAddress);
        when(modelMapper.map(updatedAddress, AddressResponse.class)).thenReturn(updatedResponse);
        
        // Act
        AddressResponse result = addressService.updateAddress(updateRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Updated Home", result.getTitle());
        assertEquals("789 Updated St, City, Country", result.getAddress());
        assertEquals(50.0, result.getLatitude());
        assertEquals(60.0, result.getLongitude());
        verify(addressRepository, times(1)).findById(1L);
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    @DisplayName("Test 2: updateAddress when address does not exist should throw VchefApiException")
    void updateAddress_WhenAddressDoesNotExist_ShouldThrowVchefApiException() {
        // Arrange
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());
        updateRequest.setId(99L);
        
        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class,
                () -> addressService.updateAddress(updateRequest));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("Address not found with id: 99"));
        verify(addressRepository, times(1)).findById(99L);
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("Test 3: updateAddress when only title is provided should update only title")
    void updateAddress_WhenOnlyTitleIsProvided_ShouldUpdateOnlyTitle() {
        // Arrange
        Address existingAddress = new Address();
        existingAddress.setId(1L);
        existingAddress.setUser(testUser);
        existingAddress.setTitle("Home");
        existingAddress.setAddress("123 Main St, City, Country");
        existingAddress.setLatitude(10.0);
        existingAddress.setLongitude(20.0);
        
        UpdateAddressRequest titleOnlyRequest = new UpdateAddressRequest();
        titleOnlyRequest.setId(1L);
        titleOnlyRequest.setTitle("New Title");
        // Address is null
        
        Address updatedAddress = new Address();
        updatedAddress.setId(1L);
        updatedAddress.setUser(testUser);
        updatedAddress.setTitle("New Title");  // Only title is updated
        updatedAddress.setAddress("123 Main St, City, Country");
        updatedAddress.setLatitude(10.0);
        updatedAddress.setLongitude(20.0);
        
        AddressResponse updatedResponse = new AddressResponse();
        updatedResponse.setId(1L);
        updatedResponse.setTitle("New Title");
        updatedResponse.setAddress("123 Main St, City, Country");
        updatedResponse.setLatitude(10.0);
        updatedResponse.setLongitude(20.0);
        
        when(addressRepository.findById(1L)).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(Address.class))).thenReturn(updatedAddress);
        when(modelMapper.map(updatedAddress, AddressResponse.class)).thenReturn(updatedResponse);
        
        // Act
        AddressResponse result = addressService.updateAddress(titleOnlyRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals("New Title", result.getTitle());
        assertEquals("123 Main St, City, Country", result.getAddress());  // Address unchanged
        assertEquals(10.0, result.getLatitude());  // Coordinates unchanged
        assertEquals(20.0, result.getLongitude());
        verify(addressRepository, times(1)).findById(1L);
        verify(addressRepository, times(1)).save(any(Address.class));
        verify(distanceService, never()).getLatLngFromAddress(anyString());  // Geocoding not called
    }

    @Test
    @DisplayName("Test 4: updateAddress when geolocation fails should propagate exception")
    void updateAddress_WhenGeolocationFails_ShouldPropagateException() {
        // Arrange
        Address existingAddress = new Address();
        existingAddress.setId(1L);
        existingAddress.setUser(testUser);
        existingAddress.setTitle("Home");
        existingAddress.setAddress("123 Main St, City, Country");
        
        when(addressRepository.findById(1L)).thenReturn(Optional.of(existingAddress));
        when(distanceService.getLatLngFromAddress(updateRequest.getAddress())).thenThrow(new RuntimeException("Geocoding error"));
        
        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class,
                () -> addressService.updateAddress(updateRequest));
        
        assertEquals("Geocoding error", exception.getMessage());
        verify(addressRepository, times(1)).findById(1L);
        verify(distanceService, times(1)).getLatLngFromAddress(anyString());
        verify(addressRepository, never()).save(any(Address.class));
    }

    // ------------------------------------------------------------------------
    // Tests for deleteAddress
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Test 1: deleteAddress when address exists should mark as deleted")
    void deleteAddress_WhenAddressExists_ShouldMarkAsDeleted() {
        // Arrange
        when(addressRepository.findById(1L)).thenReturn(Optional.of(testAddress));
        
        // Act
        addressService.deleteAddress(1L);
        
        // Assert
        verify(addressRepository, times(1)).findById(1L);
        verify(addressRepository, times(1)).save(addressCaptor.capture());
        
        // Check that isDeleted was set to true on the saved object
        assertTrue(addressCaptor.getValue().isDeleted());
    }

    @Test
    @DisplayName("Test 2: deleteAddress when address does not exist should throw VchefApiException")
    void deleteAddress_WhenAddressDoesNotExist_ShouldThrowVchefApiException() {
        // Arrange
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class,
                () -> addressService.deleteAddress(99L));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("Address not found with id: 99"));
        verify(addressRepository, times(1)).findById(99L);
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("Test 3: deleteAddress when address already deleted should still set deleted flag")
    void deleteAddress_WhenAddressAlreadyDeleted_ShouldStillSetDeletedFlag() {
        // Arrange
        testAddress.setDeleted(true);  // Already deleted
        when(addressRepository.findById(1L)).thenReturn(Optional.of(testAddress));
        
        // Act
        addressService.deleteAddress(1L);
        
        // Assert
        verify(addressRepository, times(1)).findById(1L);
        verify(addressRepository, times(1)).save(addressCaptor.capture());
        
        // It will still save the address, though the deleted flag is already true
        assertTrue(addressCaptor.getValue().isDeleted());
    }

    @Test
    @DisplayName("Test 4: deleteAddress when repository throws exception should propagate exception")
    void deleteAddress_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Arrange
        when(addressRepository.findById(1L)).thenReturn(Optional.of(testAddress));
        when(addressRepository.save(any(Address.class))).thenThrow(new RuntimeException("Database error"));
        
        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class,
                () -> addressService.deleteAddress(1L));
        
        assertEquals("Database error", exception.getMessage());
        verify(addressRepository, times(1)).findById(1L);
        verify(addressRepository, times(1)).save(any(Address.class));
    }
} 