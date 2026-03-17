package com.example.Auth_App.security;

import com.example.Auth_App.entities.User;
import com.example.Auth_App.exceptions.ResourceNotFoundException;
import com.example.Auth_App.repositories.UserRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// it implements from spring boot predefined class
@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    // this will the data from db to match for auth
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        return userRepository
                .findByEmail(username)
                .orElseThrow(() -> new BadCredentialsException("User not found with the given email id"));
    }
}
