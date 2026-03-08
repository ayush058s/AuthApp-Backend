package com.example.Auth_App.services.impl;

import com.example.Auth_App.dtos.UserDto;
import com.example.Auth_App.services.AuthService;
import com.example.Auth_App.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;

    @Override
    public UserDto registerUser(UserDto userDto) {

        // logic
        // verify email
        // verify password
        // default roles
        UserDto userDto1 = userService.createUser(userDto);
        return null;
    }
}
