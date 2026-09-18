package com.example.theatre.dto;

import jakarta.validation.constraints.*;

public record TheatreRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 255) String address,
        @NotBlank @Size(max = 100) String city
) {
}
