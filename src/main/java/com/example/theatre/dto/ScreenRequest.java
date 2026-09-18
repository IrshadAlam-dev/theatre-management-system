package com.example.theatre.dto;

import jakarta.validation.constraints.*;

public record ScreenRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Positive Integer capacity,
        @NotNull @Positive Long theatreId
) {
}
