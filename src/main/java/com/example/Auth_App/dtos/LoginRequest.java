package com.example.Auth_App.dtos;

public record LoginRequest(
        String email,
        String password
) {
}
