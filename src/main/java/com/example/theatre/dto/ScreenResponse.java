package com.example.theatre.dto;


public record ScreenResponse(
        Long id,
        String name,
        Integer capacity,
        Long theatreId
) {
}
