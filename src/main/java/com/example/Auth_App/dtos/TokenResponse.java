package com.example.Auth_App.dtos;

public record TokenResponse(String accessToken,
                            String refreshToken,
                            long expiration,
                            String tokenType,
                            UserDto user)
{
    public static TokenResponse of(String accessToken, String refreshToken, long expiration, UserDto user)
    {
        return new TokenResponse(accessToken, refreshToken, expiration, "Bearer", user);
    }
}
