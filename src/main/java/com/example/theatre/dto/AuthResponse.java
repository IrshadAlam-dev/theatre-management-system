package com.example.theatre.dto;


public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {

    @Override
    public String toString() {
        return "AuthResponse[token redacted]";
    }
}
