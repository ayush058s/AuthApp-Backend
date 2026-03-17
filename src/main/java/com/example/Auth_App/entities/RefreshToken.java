package com.example.Auth_App.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "refresh-tokens", indexes = {
    @Index(name = "refresh_tokens_jti_index", columnList = "jti", unique = true),
        @Index(name = "refresh_tokens_user_id_index", columnList = "user_id")
})
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "jti", unique = true, nullable = false, updatable = false)
    private  String jti;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(updatable = false, nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant expiredAt;

    @Column(nullable = false)
    private boolean revoked;

    private String replacedByToken;

//    private String refreshToken;
}
