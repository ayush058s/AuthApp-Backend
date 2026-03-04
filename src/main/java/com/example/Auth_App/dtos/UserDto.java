package com.example.Auth_App.dtos;

import com.example.Auth_App.entities.Provider;
import com.example.Auth_App.entities.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {

    private UUID id; // UUID => used to store unique id
    private String name;
    private String email;
    private String password;
    private String image;
    private boolean enable = true;
    private Instant createdAt = Instant.now(); // class that represents date nad time in utc format
    private Instant updatedAt = Instant.now();
    private Provider provider= Provider.LOCAL;
    private Set<RoleDto> roles = new HashSet<>();

}
