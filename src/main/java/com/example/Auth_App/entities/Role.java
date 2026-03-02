package com.example.Auth_App.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_role")
public class Role {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(unique = true, nullable = false, name = "role_name")
    private String name;
}
