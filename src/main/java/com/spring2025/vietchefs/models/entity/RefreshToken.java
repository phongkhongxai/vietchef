package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 555)
    private String token;

    private boolean expired;

    private boolean revoked;

    @OneToMany(mappedBy = "refreshToken", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AccessToken> accessTokens;

}
