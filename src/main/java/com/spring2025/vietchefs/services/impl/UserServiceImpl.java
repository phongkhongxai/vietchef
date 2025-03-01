package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Role;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.SignupDto;
import com.spring2025.vietchefs.models.payload.dto.UserDto;
import com.spring2025.vietchefs.models.payload.responseModel.UsersResponse;
import com.spring2025.vietchefs.repositories.RoleRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;



    @Override
    public UserDto saveChefUser(SignupDto signupDto) {
        if (userRepository.existsByUsername(signupDto.getUsername())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Username is already exist!");
        }
        if (userRepository.existsByEmail(signupDto.getEmail())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Email is already exist!");
        }

        User user = modelMapper.map(signupDto, User.class);

        user.setPassword(passwordEncoder.encode(signupDto.getPassword()));

        Role userRole = roleRepository.findByRoleName("ROLE_CHEF")
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User Role not found."));
        user.setRole(userRole);
        user.setAvatarUrl("default");
        user.setEmailVerified(true);
        return modelMapper.map(userRepository.save(user), UserDto.class);

    }

    @Override
    public String deleteUser(Long id) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new VchefApiException(HttpStatus.BAD_REQUEST, "User not found"));
        existingUser.setDelete(true);
        userRepository.save(existingUser);
        return "Deleted user successfully";
    }

    @Override
    public UsersResponse getAllUser(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();


        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<User> users = userRepository.findAllNotDeleted(pageable);

        List<User> userList = users.getContent();

        List<UserDto> content = userList.stream().map(bt -> modelMapper.map(bt, UserDto.class)).collect(Collectors.toList());

        UsersResponse templatesResponse = new UsersResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(users.getNumber());
        templatesResponse.setPageSize(users.getSize());
        templatesResponse.setTotalElements(users.getTotalElements());
        templatesResponse.setTotalPages(users.getTotalPages());
        templatesResponse.setLast(users.isLast());

        return templatesResponse;
    }

    @Override
    public UserDto getProfileUserByUsernameOrEmail(String username, String email) {
        User existingUser = userRepository.findByUsernameOrEmail(username, email)
                .orElseThrow(() -> new VchefApiException(HttpStatus.BAD_REQUEST, "User not found"));
        return modelMapper.map(existingUser, UserDto.class);
    }
}
