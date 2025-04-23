package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.dto.SignupDto;
import com.spring2025.vietchefs.models.payload.dto.UserDto;
import com.spring2025.vietchefs.models.payload.requestModel.ChangePasswordRequest;
import com.spring2025.vietchefs.models.payload.requestModel.UserRequest;
import com.spring2025.vietchefs.models.payload.responseModel.UsersResponse;

public interface UserService {
    //UserDto saveAdminUser(SignupDto signupDto);
    UserDto saveChefUser(SignupDto signupDto);
    String deleteUser(Long id);
    void setUserBanStatus(Long userId, boolean banned);
    UsersResponse getAllUser(int pageNo, int pageSize, String sortBy, String sortDir);
    UsersResponse getAllCustomer(int pageNo, int pageSize, String sortBy, String sortDir);
    UsersResponse getAllChef(int pageNo, int pageSize, String sortBy, String sortDir);
    UserDto getProfileUserByUsernameOrEmail(String username, String email);
    UserDto updateProfile(Long userId, UserRequest userRequest);
    void changePassword(Long userId, ChangePasswordRequest request);

}
