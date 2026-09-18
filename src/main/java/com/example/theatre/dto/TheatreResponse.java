package com.example.theatre.dto;

import java.time.LocalDateTime;

public record TheatreResponse(
        Long id,
        String name,
        String address,
        String city,
        int totalScreens,
        LocalDateTime createdAt
) {
}
