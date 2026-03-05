package com.example.Auth_App.services;

import com.example.Auth_App.dtos.UserDto;
import com.example.Auth_App.entities.Provider;
import com.example.Auth_App.entities.User;
import com.example.Auth_App.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    // as we used final so it will automatically inject construct
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    // used to convert dto into entity
    // through modelMapper library defined as bean in config

    @Override
    public UserDto createUser(UserDto userDto) {
        if(userDto.getEmail() == null ||  userDto.getEmail().isBlank()){
            throw new IllegalArgumentException("Email is required");
        }
        if(userRepository.existsByEmail(userDto.getEmail())){
            throw new IllegalArgumentException("Email already exists");
        }

        User user = modelMapper.map(userDto, User.class); // converting user dto into entity
        user.setProvider(userDto.getProvider() != null ? userDto.getProvider() : Provider.LOCAL);
        // role assign   here to user for authorization
        // TODO:
        User savedUser = userRepository.save(user);

        return modelMapper.map(savedUser, UserDto.class);
    }

    @Override
    public UserDto getUserEmail(String email) {
        return null;
    }

    @Override
    public UserDto updateUser(UserDto userDto, String userId) {
        return null;
    }

    @Override
    public void deleteUser(String userId) {

    }

    @Override
    public UserDto getUserById(String userId) {
        return null;
    }

    @Override
    public Iterable<UserDto> getAllUsers() {
        return userRepository
                .findAll()
                .stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
    }
}
