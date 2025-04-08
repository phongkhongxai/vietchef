package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.ChefTimeSettings;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.ChefTimeSettingsRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefTimeSettingsResponse;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.ChefTimeSettingsRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChefTimeSettingsServiceImplTest {

    @Mock
    private ChefTimeSettingsRepository timeSettingsRepository;

    @Mock
    private ChefRepository chefRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ChefTimeSettingsServiceImpl timeSettingsService;

    private User testUser;
    private Chef testChef;
    private ChefTimeSettings testTimeSettings;
    private ChefTimeSettingsRequest timeSettingsRequest;
    private ChefTimeSettingsResponse timeSettingsResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("Test Chef User");

        testChef = new Chef();
        testChef.setId(1L);
        testChef.setUser(testUser);

        testTimeSettings = new ChefTimeSettings();
        testTimeSettings.setSettingId(1L);
        testTimeSettings.setChef(testChef);
        testTimeSettings.setStandardPrepTime(30);
        testTimeSettings.setStandardCleanupTime(30);
        testTimeSettings.setTravelBufferPercentage(30);
        testTimeSettings.setCookingEfficiencyFactor(new BigDecimal("0.80"));
        testTimeSettings.setMinBookingNoticeHours(24);
        testTimeSettings.setMaxBookingDaysAhead(60);
        testTimeSettings.setMaxDishesPerSession(5);
        testTimeSettings.setMaxGuestsPerSession(8);
        testTimeSettings.setServiceRadiusKm(15);
        testTimeSettings.setMaxSessionsPerDay(3);

        timeSettingsRequest = new ChefTimeSettingsRequest();
        timeSettingsRequest.setStandardPrepTime(45);
        timeSettingsRequest.setStandardCleanupTime(45);
        timeSettingsRequest.setTravelBufferPercentage(40);
        timeSettingsRequest.setCookingEfficiencyFactor(new BigDecimal("0.90"));
        timeSettingsRequest.setMinBookingNoticeHours(48);
        timeSettingsRequest.setMaxBookingDaysAhead(30);
        timeSettingsRequest.setMaxDishesPerSession(4);
        timeSettingsRequest.setMaxGuestsPerSession(6);
        timeSettingsRequest.setServiceRadiusKm(10);
        timeSettingsRequest.setMaxSessionsPerDay(2);

        timeSettingsResponse = new ChefTimeSettingsResponse();
        timeSettingsResponse.setSettingId(1L);
        timeSettingsResponse.setStandardPrepTime(30);
        timeSettingsResponse.setStandardCleanupTime(30);
        timeSettingsResponse.setTravelBufferPercentage(30);
        timeSettingsResponse.setCookingEfficiencyFactor(new BigDecimal("0.80"));
        timeSettingsResponse.setMinBookingNoticeHours(24);
        timeSettingsResponse.setMaxBookingDaysAhead(60);
        timeSettingsResponse.setMaxDishesPerSession(5);
        timeSettingsResponse.setMaxGuestsPerSession(8);
        timeSettingsResponse.setServiceRadiusKm(15);
        timeSettingsResponse.setMaxSessionsPerDay(3);
    }

    @Test
    void getTimeSettingsForCurrentChef_ShouldReturnSettings_WhenSettingsExist() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            when(timeSettingsRepository.findByChef(any(Chef.class))).thenReturn(Optional.of(testTimeSettings));
            when(modelMapper.map(any(ChefTimeSettings.class), eq(ChefTimeSettingsResponse.class))).thenReturn(timeSettingsResponse);

            // Act
            ChefTimeSettingsResponse result = timeSettingsService.getTimeSettingsForCurrentChef();

            // Assert
            assertNotNull(result);
            assertEquals(timeSettingsResponse.getSettingId(), result.getSettingId());
            assertEquals(timeSettingsResponse.getStandardPrepTime(), result.getStandardPrepTime());
            verify(timeSettingsRepository).findByChef(testChef);
        }
    }

    @Test
    void getTimeSettingsForCurrentChef_ShouldCreateNewSettings_WhenSettingsDoNotExist() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            when(timeSettingsRepository.findByChef(any(Chef.class))).thenReturn(Optional.empty());
            when(timeSettingsRepository.save(any(ChefTimeSettings.class))).thenReturn(testTimeSettings);
            when(modelMapper.map(any(ChefTimeSettings.class), eq(ChefTimeSettingsResponse.class))).thenReturn(timeSettingsResponse);

            // Act
            ChefTimeSettingsResponse result = timeSettingsService.getTimeSettingsForCurrentChef();

            // Assert
            assertNotNull(result);
            verify(timeSettingsRepository).save(any(ChefTimeSettings.class));
        }
    }

    @Test
    void updateTimeSettingsForCurrentChef_ShouldReturnUpdatedSettings() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            when(timeSettingsRepository.findByChef(any(Chef.class))).thenReturn(Optional.of(testTimeSettings));
            when(timeSettingsRepository.save(any(ChefTimeSettings.class))).thenReturn(testTimeSettings);
            when(modelMapper.map(any(ChefTimeSettings.class), eq(ChefTimeSettingsResponse.class))).thenReturn(timeSettingsResponse);

            // Act
            ChefTimeSettingsResponse result = timeSettingsService.updateTimeSettingsForCurrentChef(timeSettingsRequest);

            // Assert
            assertNotNull(result);
            verify(timeSettingsRepository).save(testTimeSettings);
            
            // Verify the settings were updated with values from the request
            assertEquals(timeSettingsRequest.getStandardPrepTime(), testTimeSettings.getStandardPrepTime());
            assertEquals(timeSettingsRequest.getStandardCleanupTime(), testTimeSettings.getStandardCleanupTime());
            assertEquals(timeSettingsRequest.getTravelBufferPercentage(), testTimeSettings.getTravelBufferPercentage());
            assertEquals(timeSettingsRequest.getCookingEfficiencyFactor(), testTimeSettings.getCookingEfficiencyFactor());
            assertEquals(timeSettingsRequest.getMinBookingNoticeHours(), testTimeSettings.getMinBookingNoticeHours());
            assertEquals(timeSettingsRequest.getMaxBookingDaysAhead(), testTimeSettings.getMaxBookingDaysAhead());
            assertEquals(timeSettingsRequest.getMaxDishesPerSession(), testTimeSettings.getMaxDishesPerSession());
            assertEquals(timeSettingsRequest.getMaxGuestsPerSession(), testTimeSettings.getMaxGuestsPerSession());
            assertEquals(timeSettingsRequest.getServiceRadiusKm(), testTimeSettings.getServiceRadiusKm());
            assertEquals(timeSettingsRequest.getMaxSessionsPerDay(), testTimeSettings.getMaxSessionsPerDay());
        }
    }

    @Test
    void updateTimeSettingsForCurrentChef_ShouldCreateNewSettings_WhenSettingsDoNotExist() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            when(timeSettingsRepository.findByChef(any(Chef.class))).thenReturn(Optional.empty());
            when(timeSettingsRepository.save(any(ChefTimeSettings.class))).thenReturn(testTimeSettings);
            when(modelMapper.map(any(ChefTimeSettings.class), eq(ChefTimeSettingsResponse.class))).thenReturn(timeSettingsResponse);

            // Act
            ChefTimeSettingsResponse result = timeSettingsService.updateTimeSettingsForCurrentChef(timeSettingsRequest);

            // Assert
            assertNotNull(result);
            verify(timeSettingsRepository, times(2)).save(any(ChefTimeSettings.class));
        }
    }

    @Test
    void resetTimeSettingsToDefault_ShouldResetToDefaultValues() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            when(timeSettingsRepository.findByChef(any(Chef.class))).thenReturn(Optional.of(testTimeSettings));
            when(timeSettingsRepository.save(any(ChefTimeSettings.class))).thenReturn(testTimeSettings);
            when(modelMapper.map(any(ChefTimeSettings.class), eq(ChefTimeSettingsResponse.class))).thenReturn(timeSettingsResponse);

            // Act
            ChefTimeSettingsResponse result = timeSettingsService.resetTimeSettingsToDefault();

            // Assert
            assertNotNull(result);
            verify(timeSettingsRepository).save(testTimeSettings);
            
            // Verify default values were set
            assertEquals(30, testTimeSettings.getStandardPrepTime());
            assertEquals(30, testTimeSettings.getStandardCleanupTime());
            assertEquals(30, testTimeSettings.getTravelBufferPercentage());
            assertEquals(new BigDecimal("0.80"), testTimeSettings.getCookingEfficiencyFactor());
            assertEquals(24, testTimeSettings.getMinBookingNoticeHours());
            assertEquals(60, testTimeSettings.getMaxBookingDaysAhead());
            assertEquals(5, testTimeSettings.getMaxDishesPerSession());
            assertEquals(8, testTimeSettings.getMaxGuestsPerSession());
            assertEquals(15, testTimeSettings.getServiceRadiusKm());
            assertEquals(3, testTimeSettings.getMaxSessionsPerDay());
        }
    }

    @Test
    void getTimeSettingsById_ShouldReturnSettings_WhenSettingsExistAndCurrentChefIsOwner() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(timeSettingsRepository.findById(anyLong())).thenReturn(Optional.of(testTimeSettings));
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            when(modelMapper.map(any(ChefTimeSettings.class), eq(ChefTimeSettingsResponse.class))).thenReturn(timeSettingsResponse);

            // Act
            ChefTimeSettingsResponse result = timeSettingsService.getTimeSettingsById(1L);

            // Assert
            assertNotNull(result);
            assertEquals(timeSettingsResponse.getSettingId(), result.getSettingId());
            verify(timeSettingsRepository).findById(1L);
        }
    }

    @Test
    void getTimeSettingsById_ShouldThrowException_WhenSettingsNotFound() {
        // Arrange
        when(timeSettingsRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            timeSettingsService.getTimeSettingsById(999L);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void getTimeSettingsById_ShouldThrowException_WhenCurrentChefIsNotOwner() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            // Create a different chef as owner
            Chef differentChef = new Chef();
            differentChef.setId(2L);
            
            ChefTimeSettings settings = new ChefTimeSettings();
            settings.setSettingId(1L);
            settings.setChef(differentChef);
            
            when(timeSettingsRepository.findById(anyLong())).thenReturn(Optional.of(settings));
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));

            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                timeSettingsService.getTimeSettingsById(1L);
            });
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }
}