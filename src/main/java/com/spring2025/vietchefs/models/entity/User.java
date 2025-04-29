package com.spring2025.vietchefs.models.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="user_id")
    private Long id;

    private String uid;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    @Column(nullable = false)
    private String gender;

    @Column(unique = true)
    private String phone;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;
    @Column
    private String avatarUrl;
    @Column
    private LocalDateTime premiumExpiryDate;
    @Column(nullable = false)
    private boolean isPremium = false;
    @Column(nullable = false)
    private boolean isDelete = false;
    @Column(nullable = false)
    private boolean isBanned = false;
    @Column(nullable = false)
    private boolean emailVerified;

    @Column(nullable = true)
    private String verificationCode;

    @Column(nullable = true)
    private LocalDateTime verificationCodeExpiry;

    @Column(nullable = true)
    private String resetPasswordToken;

    @Column(nullable = true)
    private LocalDateTime resetPasswordExpiry;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    @OneToMany(mappedBy = "user")
    private List<AccessToken> accessToken;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses;

    @OneToOne(mappedBy = "user")
    private Chef chef;
    @Column(name = "expo_token")
    private String expoToken;

}
