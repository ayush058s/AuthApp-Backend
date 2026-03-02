package com.example.Auth_App.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder // search why it is used?????????????


@Entity
@Table(name = "users") // if we want to name table different than class
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // used to autoIncrement
    private UUID id; // UUID => used to store unique id

    @Column(length = 100, name = "user_name")
    private String name;

    @Column(name = "user_email", unique = true) // if we want some other name in db table
    private String email;
    private String password;
    private String image;
    private boolean enable = true;
    private Instant createdAt = Instant.now(); // class that represents date nad time in utc format
    private Instant updatedAt = Instant.now();

    @Enumerated(EnumType.STRING) // as it is an enum class
    private Provider provider= Provider.LOCAL;

    @ManyToMany(fetch =  FetchType.EAGER) // it will define relationship with ROLE CLASS and
    // in database it will create a third table(JOIN TABLE) defining their relationship
    //if we want to customize name
    @JoinTable(name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")) // this will be done auto
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate(){
        Instant now = Instant.now();
        if(createdAt == null){
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = Instant.now();
    }

}
