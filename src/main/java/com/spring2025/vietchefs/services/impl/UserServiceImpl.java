package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.Role;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.SignupDto;
import com.spring2025.vietchefs.models.payload.dto.UserDto;
import com.spring2025.vietchefs.models.payload.requestModel.ChangePasswordRequest;
import com.spring2025.vietchefs.models.payload.requestModel.UserRequest;
import com.spring2025.vietchefs.models.payload.responseModel.UserResponse;
import com.spring2025.vietchefs.models.payload.responseModel.UsersResponse;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.RoleRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.UserService;
import jakarta.transaction.Transactional;
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

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    private ChefRepository chefRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ImageService imageService;



    @Override
    public UserDto saveChefUser(SignupDto signupDto) {
        if (userRepository.existsByUsername(signupDto.getUsername())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Username is already exist!");
        }
        if (userRepository.existsByEmail(signupDto.getEmail())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Email is already exist!");
        }

        User user = modelMapper.map(signupDto, User.class);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
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
    public void setUserBanStatus(Long userId, boolean banned) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));
        user.setBanned(banned);
        if (user.getChef() != null) {
            Chef chef = user.getChef();
            chef.setStatus(banned ? "BANNED" : "ACTIVE");
            chefRepository.save(chef);
        }
        userRepository.save(user);
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
    public UsersResponse getAllCustomer(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();


        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<User> users = userRepository.findByRoleNameAndIsDeleteFalse("ROLE_CUSTOMER",pageable);

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
    public UsersResponse getAllChef(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();


        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<User> users = userRepository.findByRoleNameAndIsDeleteFalse("ROLE_CHEF",pageable);

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

    @Override
    public UserResponse getProfileUserByUsername(String username) {
        User existingUser = userRepository.findByUsernameOrEmail(username, null)
                .orElseThrow(() -> new VchefApiException(HttpStatus.BAD_REQUEST, "User not found"));
        return modelMapper.map(existingUser, UserResponse.class);
    }

    @Override
    @Transactional
    public UserDto updateProfile(Long userId, UserRequest userRequest) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId);
        }
        User user = userOptional.get();

        // Cập nhật thông tin từ UserDTO nếu có
        user.setFullName(userRequest.getFullName() != null ? userRequest.getFullName() : user.getFullName());
        user.setDob(userRequest.getDob() != null ? userRequest.getDob() : user.getDob());
        user.setGender(userRequest.getGender() != null ? userRequest.getGender() : user.getGender());
        user.setPhone(userRequest.getPhone() != null ? userRequest.getPhone() : user.getPhone());
        User updatedUser = userRepository.save(user);
        if (userRequest.getFile() != null && !userRequest.getFile().isEmpty()) {
            try{
                String avatarUrl = imageService.uploadImage(userRequest.getFile(), userId, "USER");
                updatedUser.setAvatarUrl(avatarUrl);
            } catch (IOException e) {
                throw new VchefApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload image.");
            }
        }
        return modelMapper.map(updatedUser, UserDto.class);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "New password and confirm password do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
