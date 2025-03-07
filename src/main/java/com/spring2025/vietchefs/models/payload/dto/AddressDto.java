package com.spring2025.vietchefs.models.payload.dto;

import com.spring2025.vietchefs.models.entity.User;
import jakarta.persistence.*;

public class AddressDto {

    private Long id;

    private UserDto user;

    private String title;

    private String address;

}
