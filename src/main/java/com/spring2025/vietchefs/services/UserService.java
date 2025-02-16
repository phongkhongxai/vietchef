package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.dto.SignupDto;
import com.spring2025.vietchefs.models.payload.dto.UserDto;
import com.spring2025.vietchefs.models.payload.responseModel.UsersResponse;

public interface UserService {
    //UserDto saveAdminUser(SignupDto signupDto);
    UserDto saveChefUser(SignupDto signupDto);
    String deleteUser(Long id);
    UsersResponse getAllUser(int pageNo, int pageSize, String sortBy, String sortDir);

}
