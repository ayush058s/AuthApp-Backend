package com.example.Auth_App.services;

import com.example.Auth_App.dtos.UserDto;

public interface AuthService {
    UserDto registerUser(UserDto userDto);

}
