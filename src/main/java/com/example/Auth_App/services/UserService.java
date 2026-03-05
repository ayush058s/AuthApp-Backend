package com.example.Auth_App.services;

import com.example.Auth_App.dtos.UserDto;

public interface UserService {
    // create user
    UserDto createUser(UserDto userDto);

    // get user by email
    UserDto getUserEmail(String email);

    // update user
    UserDto updateUser(UserDto userDto, String userId);

    // delete user
    void deleteUser(String userId);

    // get user by id
    UserDto getUserById(String userId);

    // get all users
    Iterable<UserDto> getAllUsers();

}
