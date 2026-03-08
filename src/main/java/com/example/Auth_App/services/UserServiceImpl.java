package com.example.Auth_App.services;

import com.example.Auth_App.dtos.UserDto;
import com.example.Auth_App.entities.Provider;
import com.example.Auth_App.entities.User;
import com.example.Auth_App.exceptions.ResourceNotFoundException;
import com.example.Auth_App.helpers.UserHelpers;
import com.example.Auth_App.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    // as we used final so it will automatically inject construct
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    // used to convert dto into entity
    // through modelMapper library defined as bean in config

    @Override
    @Transactional
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
        User user = userRepository
                .findByEmail(email).
                orElseThrow(() ->  new ResourceNotFoundException("User Not Found with give email id"));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public UserDto updateUser(UserDto userDto, String userId) {
        UUID uId = UserHelpers.parseUUID(userId);
        User existingUser = userRepository.findById(uId).orElseThrow(() -> new ResourceNotFoundException("User Not Found with give id"));

        if(userDto.getName() != null){
            existingUser.setName(userDto.getName());
        }
        if(userDto.getProvider() != null) {
            existingUser.setProvider(userDto.getProvider());
        }

        if(userDto.getImage() != null){
            existingUser.setImage(userDto.getImage());
        }
        //  TODO: change the password update logic
        if(userDto.getPassword() != null){
            existingUser.setPassword(userDto.getPassword());
        }

        existingUser.setEnable(userDto.isEnable());

        User updatedUser = userRepository.save(existingUser);
        return modelMapper.map(updatedUser, UserDto.class);
    }

    @Override
    public void deleteUser(String userId) {
        // before writing this convert string id into uuid with the UserHelper in helpers
        UUID uId = UserHelpers.parseUUID(userId);
        User user = userRepository.findById(uId).orElseThrow(() -> new ResourceNotFoundException("User Not Found with give id"));
        userRepository.delete(user);
    }

    @Override
    public UserDto getUserById(String userId) {
        User user = userRepository.findById(UserHelpers.parseUUID(userId)).orElseThrow(() -> new ResourceNotFoundException("User Not Found with give id"));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    @Transactional
    public Iterable<UserDto> getAllUsers() {
        return userRepository
                .findAll()
                .stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
    }
}
