package com.example.Auth_App.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


import java.time.Instant;
import java.util.*;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder // search why it is used?????????????


@Entity
@Table(name = "users") // if we want to name table different than class
public class User  implements UserDetails {

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
    private String providerId;

    @ManyToMany(fetch =  FetchType.EAGER) // it will define relationship with ROLE CLASS and
    // in database it will create a third table(JOIN TABLE) defining their relationship
    //if we want to customize name
    @JoinTable(name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")) // this will be done auto
    private Set<Role> roles = new HashSet<>();



    @PrePersist
    protected void onCreate(){
        Instant now = Instant.now(); // Creates the current timestamp It represents UTC time.
        if(createdAt == null){
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = Instant.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return roles
                .stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .toList();
    }

    @Override
    public String getUsername() {
        return this.email; // will use email to as username for login
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.enable;
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
